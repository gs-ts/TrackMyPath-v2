package gts.trackmypath.ui.activepath

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gts.trackmypath.domain.photo.ObserveFetchedPhotoMetadataUseCase
import gts.trackmypath.domain.photo.PhotoMetadata
import gts.trackmypath.domain.route.FinishRouteUseCase
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivePathViewModel @Inject constructor(
    private val serviceStateHolder: ServiceStateHolder,
    private val observeFetchedPhotoMetadataUseCase: ObserveFetchedPhotoMetadataUseCase,
    private val startRouteUseCase: StartRouteUseCase,
    private val finishRouteUseCase: FinishRouteUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var locationUpdatesJob: Job? = null

    init {
        observeServiceEvents()
        observeFetchedPhotos()
    }

    private fun observeFetchedPhotos() {
        viewModelScope.launch {
            observeFetchedPhotoMetadataUseCase.photos.collect { photos ->
                _state.update { state ->
                    state.copy(photos = photos.toPersistentList())
                }
            }
        }
    }

    fun onTrackPathClick() {
        val currentTrackingState = _state.value.trackingState

        when (currentTrackingState) {
            TrackingState.STOPPED -> {
                viewModelScope.launch {
                    // TODO: what if app (and service) gets killed by the system,
                    //  the database entry is there! You must delete it.
                    val newRouteId = startRouteUseCase()

                    _state.update { state ->
                        state.copy(
                            ongoingRouteId = newRouteId,
                            trackingState = TrackingState.STARTED,
                        )
                    }
                }
            }

            TrackingState.STARTED -> {
                _state.update { state ->
                    state.copy(
                        trackingState = TrackingState.STOPPED,
                        showNameRouteDialog = true
                    )
                }
            }
        }
    }

    fun onRouteNameChange(newRouteName: String) {
        _state.update { state -> state.copy(routeNameInput = newRouteName) }
    }

    fun onConfirmNameRouteDialogClick() {
        val routeId = _state.value.ongoingRouteId
        val routeName = _state.value.routeNameInput
        routeId?.let {
            viewModelScope.launch {
                finishRouteUseCase(
                    routeId = routeId,
                    displayName = routeName
                )
            }
        }

        _state.update { state ->
            state.copy(
                ongoingRouteId = null,
                showNameRouteDialog = false,
                routeNameInput = "",
                photos = persistentListOf() // clear the photo stream
            )
        }
    }

    fun onDismissNameRouteDialogClick() {
        _state.update { state ->
            state.copy(
                showNameRouteDialog = false,
                routeNameInput = "",
                photos = persistentListOf() // clear the photo stream
            )
        }
        // Note: Since we have a "pending route" in the DB, you will want to call
        // your DeleteRouteUseCase or save it as "Unnamed Route" here.
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeServiceEvents() {
        viewModelScope.launch {
            serviceStateHolder.isServiceRunning
                .collectLatest { isServiceRunning ->
                    if (isServiceRunning) {
                        if (state.value.trackingState == TrackingState.STOPPED) {
                            _state.update { state ->
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
        _state.update { state ->
            state.copy(
                trackingState = TrackingState.STOPPED,
                photos = persistentListOf()
            )
        }
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
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
