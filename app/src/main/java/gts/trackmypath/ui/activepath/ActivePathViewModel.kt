package gts.trackmypath.ui.activepath

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.domain.route.DeleteRouteWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.FinishRouteUseCase
import gts.trackmypath.domain.route.ObserveRouteWithPhotoMetadataContract
import gts.trackmypath.domain.route.RenameRouteUseCase
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.StartRouteUseCase
import gts.trackmypath.domain.route.normalizeRouteName
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
    private val renameRouteUseCase: RenameRouteUseCase,
    private val deleteRouteWithPhotoMetadataUseCase: DeleteRouteWithPhotoMetadataUseCase,
    private val observeRouteWithPhotoMetadataUseCase: ObserveRouteWithPhotoMetadataContract
) : ViewModel() {

    val state: StateFlow<State>
        field = MutableStateFlow(State())

    private var ongoingRoutePhotosJob: Job? = null

    // True from a tap on Start until the new route is created and handed to the service.
    private var isStartingRoute = false

    init {
        observeLocationServiceEvents()
    }

    fun onAction(action: Action) {
        when (action) {
            Action.OnStartTrackPathClick -> onStartTrackPathClick()
            Action.OnStopTrackPathClick -> onStopTrackPathClick()
            Action.OnPlaceFiltersClick -> onPlaceFilterClick()
            Action.OnClosePlaceFilterBottomSheet -> onClosePlaceFilterBottomSheet()
            is Action.OnRouteNameChange -> onRouteNameChange(action.routeName)
            Action.OnConfirmNameRouteDialogClick -> onConfirmNameRouteDialogClick()
            Action.OnDiscardRouteClick -> onDiscardRouteClick()
            Action.HideSnackbarRouteSavedConfirmation -> hideSnackbarRouteSavedConfirmation()
        }
    }

    fun onStartTrackPathClick() {
        // A second tap while the first one is still creating the route would start a second route.
        if (isStartingRoute || state.value.isTracking) return
        isStartingRoute = true
        viewModelScope.launch {
            try {
                // ongoingRouteId isn't set here: it comes from the service, which reports the route it tracks.
                locationServiceManager.startTracking(routeId = startRouteUseCase())
            } finally {
                isStartingRoute = false
            }
        }
    }

    fun onStopTrackPathClick() {
        locationServiceManager.stopTracking()
        val routeId = state.value.ongoingRouteId ?: return
        state.update { state ->
            state.copy(shouldShowNameRouteDialog = true)
        }
        // Finish the route before the user names it, so it's kept even if the app is closed
        // or killed while the dialog is open.
        viewModelScope.launch {
            finishRouteUseCase(routeId = routeId)
        }
    }

    fun onPlaceFilterClick() {
        state.update { state ->
            state.copy(shouldShowPlaceFilterBottomSheet = true)
        }
    }

    fun onClosePlaceFilterBottomSheet() {
        state.update { state -> state.copy(shouldShowPlaceFilterBottomSheet = false) }
    }

    fun onRouteNameChange(newRouteName: String) {
        state.update { state -> state.copy(routeNameInput = newRouteName) }
    }

    fun onConfirmNameRouteDialogClick() {
        // The Confirm button is enabled only for a valid name.
        val routeName = normalizeRouteName(state.value.routeNameInput) ?: return
        val routeId = state.value.ongoingRouteId
        if (routeId != null) {
            viewModelScope.launch {
                renameRouteUseCase(routeId = routeId, name = routeName)
            }
        }

        state.update { state ->
            state.withNameRouteDialogClosed().copy(shouldShowSnackbarRouteSavedConfirmation = true)
        }
    }

    private fun onDiscardRouteClick() {
        val routeId = state.value.ongoingRouteId
        if (routeId != null) {
            viewModelScope.launch {
                deleteRouteWithPhotoMetadataUseCase(routeId = routeId)
            }
        }

        state.update { state -> state.withNameRouteDialogClosed() }
    }

    fun hideSnackbarRouteSavedConfirmation() {
        state.update { state ->
            state.copy(shouldShowSnackbarRouteSavedConfirmation = false)
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
                        ongoingRoutePhotosJob?.cancel()
                        ongoingRoutePhotosJob = null
                    }
                }
        }
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
        private val isLocationServiceRunning: Boolean = false,
        val ongoingRouteId: RouteId? = null,
        val photos: PersistentList<PhotoMetadata> = persistentListOf(),
        val shouldShowPlaceFilterBottomSheet: Boolean = false,
        val routeNameInput: String = "",
        val shouldShowNameRouteDialog: Boolean = false,
        val shouldShowSnackbarRouteSavedConfirmation: Boolean = false
    ) {

        val isTracking: Boolean
            get() = isLocationServiceRunning && ongoingRouteId != null

        val isStreamLoading: Boolean
            get() = isTracking && photos.isEmpty()
    }

    sealed interface Action {
        data object OnStartTrackPathClick : Action

        data object OnStopTrackPathClick : Action

        data object OnPlaceFiltersClick : Action

        data object OnClosePlaceFilterBottomSheet : Action

        data class OnRouteNameChange(val routeName: String) : Action

        data object OnConfirmNameRouteDialogClick : Action

        data object OnDiscardRouteClick : Action

        data object HideSnackbarRouteSavedConfirmation : Action
    }
}

private fun ActivePathViewModel.State.withNameRouteDialogClosed() = copy(
    ongoingRouteId = null,
    shouldShowNameRouteDialog = false,
    routeNameInput = "",
    photos = persistentListOf() // clear the photo stream
)
