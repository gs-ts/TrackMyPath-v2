package gts.trackmypath.ui.activepath

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.domain.route.DeleteRouteWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.FinishRouteUseCase
import gts.trackmypath.domain.route.ObserveRouteWithPhotoMetadataContract
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.StartRouteUseCase
import gts.trackmypath.ui.service.LocationServiceManager
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
    private val locationServiceManager: LocationServiceManager,
    private val startRouteUseCase: StartRouteUseCase,
    private val finishRouteUseCase: FinishRouteUseCase,
    private val deleteRouteWithPhotoMetadataUseCase: DeleteRouteWithPhotoMetadataUseCase,
    private val observeRouteWithPhotoMetadataUseCase: ObserveRouteWithPhotoMetadataContract
) : ViewModel() {

    val state: StateFlow<State>
        field = MutableStateFlow(State())

    private var ongoingRoutePhotosJob: Job? = null

    init {
        observeLocationServiceEvents()
    }

    fun onStartTrackPathClick() {
        viewModelScope.launch {
            val newRouteId = startRouteUseCase()
            state.update { state ->
                state.copy(ongoingRouteId = newRouteId)
            }
            locationServiceManager.startTracking(routeId = newRouteId)
        }
    }

    fun onStopTrackPathClick() {
        locationServiceManager.stopTracking()
        state.update { state ->
            state.copy(showNameRouteDialog = true)
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
                photos = persistentListOf(), // clear the photo stream
                showSnackbarRouteSavedConfirmation = true
            )
        }
    }

    fun onDismissNameRouteDialogClick() {
        viewModelScope.launch {
            val routeId = state.value.ongoingRouteId
            routeId?.let {
                deleteRouteWithPhotoMetadataUseCase(routeId = routeId)
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

    fun onHideSnackbarRouteSavedConfirmation() {
        state.update { state ->
            state.copy(showSnackbarRouteSavedConfirmation = false)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeLocationServiceEvents() {
        viewModelScope.launch {
            locationServiceManager.trackingState
                .collectLatest { trackingState ->
                    Log.d("ActivePathViewModel", "trackingState: $trackingState")
                    if (trackingState.isRunning) {
                        state.update { state ->
                            state.copy(
                                isLocationServiceRunning = true,
                                ongoingRouteId = trackingState.activeRouteId
                            )
                        }
                        ongoingRoutePhotosJob?.cancel()
                        observeOngoingRoutePhotos()
                    } else {
                        state.update { state ->
                            state.copy(isLocationServiceRunning = false)
                        }
                        stopLocationUpdates()
                    }
                }
        }
    }

    private fun stopLocationUpdates() {
        Log.d("ActivePathViewModel", "user clicked stop location updates")
        ongoingRoutePhotosJob?.cancel()
        ongoingRoutePhotosJob = null
    }

    private fun observeOngoingRoutePhotos() {
        state.value.ongoingRouteId?.let { ongoingRouteId ->
            ongoingRoutePhotosJob = viewModelScope.launch {
                observeRouteWithPhotoMetadataUseCase(routeId = ongoingRouteId)
                    .collect { routeWithPhotoMetadata ->
                        Log.d("ActivePathViewModel", "routeWithPhotoMetadata received: $routeWithPhotoMetadata")
                        state.update { currentState ->
                            currentState.copy(photos = routeWithPhotoMetadata.photoMetadata.toPersistentList())
                        }
                    }
            }
        }
    }

    data class State(
        val isLocationServiceRunning: Boolean = false,
        val ongoingRouteId: RouteId? = null,
        val photos: PersistentList<PhotoMetadata> = persistentListOf(),
        val routeNameInput: String = "",
        val showNameRouteDialog: Boolean = false,
        val showSnackbarRouteSavedConfirmation: Boolean = false
    ) {

        val isTracking: Boolean
            get() = isLocationServiceRunning && ongoingRouteId != null
    }
}
