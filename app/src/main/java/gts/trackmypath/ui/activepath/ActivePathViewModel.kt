package gts.trackmypath.ui.activepath

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.domain.route.DeletePendingRouteUseCase
import gts.trackmypath.domain.route.FinishRouteUseCase
import gts.trackmypath.domain.route.ObserveRouteWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.StartRouteUseCase
import gts.trackmypath.ui.activepath.ActivePathViewModel.State.TrackingState
import gts.trackmypath.ui.service.ServiceStateHolder
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivePathViewModel @Inject constructor(
    private val serviceStateHolder: ServiceStateHolder,
    private val startRouteUseCase: StartRouteUseCase,
    private val finishRouteUseCase: FinishRouteUseCase,
    private val deletePendingRouteUseCase: DeletePendingRouteUseCase,
    private val observeRouteWithPhotoMetadataUseCase: ObserveRouteWithPhotoMetadataUseCase
) : ViewModel() {

    val state: StateFlow<State>
        field = MutableStateFlow(State())

    private var locationUpdatesJob: Job? = null
    private var ongoingRoutePhotosJob: Job? = null

    init {
        observeServiceEvents()
    }

    fun onTrackPathClick() {
        val currentTrackingState = state.value.trackingState

        when (currentTrackingState) {
            TrackingState.STOPPED -> {
                viewModelScope.launch {
                    val newRouteId = startRouteUseCase()
                    state.update { state ->
                        state.copy(
                            ongoingRouteId = newRouteId,
                            trackingState = TrackingState.STARTED,
                        )
                    }
                    observeOngoingRoutePhotos()
                }
            }

            TrackingState.STARTED -> {
                ongoingRoutePhotosJob?.cancel()

                state.update { state ->
                    state.copy(
                        trackingState = TrackingState.STOPPED,
                        showNameRouteDialog = true
                    )
                }
            }
        }
    }

    fun onRouteNameChange(newRouteName: String) {
        state.update { state -> state.copy(routeNameInput = newRouteName) }
    }

    fun onConfirmNameRouteDialogClick() {
        val routeId = state.value.ongoingRouteId
        val routeName = state.value.routeNameInput
        routeId?.let {
            viewModelScope.launch {
                finishRouteUseCase(
                    routeId = routeId,
                    displayName = routeName
                )
            }
        }

        state.update { state ->
            state.copy(
                ongoingRouteId = null,
                showNameRouteDialog = false,
                routeNameInput = "",
                photos = persistentListOf() // clear the photo stream
            )
        }
    }

    fun onDismissNameRouteDialogClick() {
        viewModelScope.launch {
            val routeId = state.value.ongoingRouteId
            routeId?.let {
                deletePendingRouteUseCase(routeId = routeId)
            }

            state.update { state ->
                state.copy(
                    ongoingRouteId = null,
                    showNameRouteDialog = false,
                    routeNameInput = "",
                    photos = persistentListOf() // clear the photo stream
                )
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeServiceEvents() {
        viewModelScope.launch {
            serviceStateHolder.isServiceRunning
                .collectLatest { isServiceRunning ->
                    if (isServiceRunning) {
                        if (state.value.trackingState == TrackingState.STOPPED) {
                            state.update { state ->
                                state.copy(trackingState = TrackingState.STARTED)
                            }
                        }
                    } else {
                        stopLocationUpdates()
                    }
                }
        }
    }

    private fun stopLocationUpdates() {
        Log.d("ActivePathViewModel", "user clicked stop location updates")
        state.update { state ->
            state.copy(trackingState = TrackingState.STOPPED)
        }
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
    }

    private fun observeOngoingRoutePhotos() {
        state.value.ongoingRouteId?.let {
            ongoingRoutePhotosJob = viewModelScope.launch {
                observeRouteWithPhotoMetadataUseCase(routeId = it).collect { routeWithPhotoMetadata ->
                    state.update { state ->
                        state.copy(photos = routeWithPhotoMetadata.photoMetadata.toPersistentList())
                    }
                }
            }
        }
    }

    data class State(
        val trackingState: TrackingState = TrackingState.STOPPED,
        val photos: PersistentList<PhotoMetadata> = persistentListOf(),
        val showNameRouteDialog: Boolean = false,
        val ongoingRouteId: RouteId? = null,
        val routeNameInput: String = ""
    ) {

        enum class TrackingState {
            STARTED,
            STOPPED,
        }
    }
}
