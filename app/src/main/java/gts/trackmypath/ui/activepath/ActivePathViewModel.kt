package gts.trackmypath.ui.activepath

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.domain.route.DeleteRouteWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.ObserveRouteWithPhotoMetadataContract
import gts.trackmypath.domain.route.RenameRouteUseCase
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.normalizeRouteName
import gts.trackmypath.domain.tracking.RouteTracker
import gts.trackmypath.domain.tracking.StartTrackingUseCase
import gts.trackmypath.domain.tracking.StopTrackingUseCase
import gts.trackmypath.domain.tracking.TrackingState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivePathViewModel @Inject constructor(
    private val routeTracker: RouteTracker,
    private val startTrackingUseCase: StartTrackingUseCase,
    private val stopTrackingUseCase: StopTrackingUseCase,
    private val renameRouteUseCase: RenameRouteUseCase,
    private val deleteRouteWithPhotoMetadataUseCase: DeleteRouteWithPhotoMetadataUseCase,
    private val observeRouteWithPhotoMetadataUseCase: ObserveRouteWithPhotoMetadataContract
) : ViewModel() {

    // What only this screen knows. The tracking state comes from routeTracker, and the photos from the database.
    private val screenState = MutableStateFlow(ScreenState())

    // The photos of the route on screen. The database query restarts only when that route changes,
    // not on every screen state change (e.g. typing a name).
    @OptIn(ExperimentalCoroutinesApi::class)
    private val routePhotos: Flow<RoutePhotos?> = combine(
        routeTracker.state,
        screenState,
        ::routeOnScreen
    )
        .distinctUntilChanged()
        .flatMapLatest { routeId ->
            if (routeId == null) {
                flowOf(null)
            } else {
                observeRouteWithPhotoMetadataUseCase(routeId = routeId)
                    .map { route ->
                        RoutePhotos(
                            routeId = routeId,
                            photos = route.photoMetadata.toPersistentList()
                        )
                    }
                    // null until the photos load, so the screen state doesn't wait for the database
                    .onStart<RoutePhotos?> { emit(null) }
            }
        }

    val state: StateFlow<State> = combine(
        routeTracker.state,
        screenState,
        routePhotos,
        ::toState
    )
        .stateIn(
            scope = viewModelScope,
            // Not WhileSubscribed: the photos stay loaded while the user is on another screen, as they did
            // before, so coming back doesn't show an empty stream first.
            started = SharingStarted.Eagerly,
            // combine emits a moment later. Starting from the tracker's current state means that reopening
            // the app while tracking doesn't show the Start button first.
            initialValue = toState(routeTracker.state.value, screenState.value, routePhotos = null),
        )

    // True from a tap on Start until the new route is created and handed to the tracker.
    private var isStartingRoute = false

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
        if (isStartingRoute || routeTracker.state.value is TrackingState.Tracking) return
        isStartingRoute = true
        viewModelScope.launch {
            try {
                // The screen shows the new route when the tracker reports that it tracks it.
                startTrackingUseCase()
            } finally {
                isStartingRoute = false
            }
        }
    }

    fun onStopTrackPathClick() {
        // The tracker reports Idle a moment after Stop. A second tap before that would finish the route again.
        if (screenState.value.routeAwaitingName != null) return
        val routeId = when (val tracking = routeTracker.state.value) {
            is TrackingState.Tracking -> tracking.routeId
            TrackingState.Idle -> return
        }
        screenState.update { screen -> screen.copy(routeAwaitingName = routeId) }
        viewModelScope.launch {
            stopTrackingUseCase(routeId = routeId)
        }
    }

    fun onPlaceFilterClick() {
        screenState.update { screen -> screen.copy(shouldShowPlaceFilterBottomSheet = true) }
    }

    fun onClosePlaceFilterBottomSheet() {
        screenState.update { screen -> screen.copy(shouldShowPlaceFilterBottomSheet = false) }
    }

    fun onRouteNameChange(newRouteName: String) {
        screenState.update { screen -> screen.copy(routeNameInput = newRouteName) }
    }

    fun onConfirmNameRouteDialogClick() {
        // The Confirm button is enabled only for a valid name, and the dialog is shown only for a route.
        val routeName = normalizeRouteName(screenState.value.routeNameInput) ?: return
        val routeId = screenState.value.routeAwaitingName ?: return
        screenState.update { screen ->
            screen.withNameRouteDialogClosed().copy(shouldShowSnackbarRouteSavedConfirmation = true)
        }
        viewModelScope.launch {
            renameRouteUseCase(routeId = routeId, name = routeName)
        }
    }

    private fun onDiscardRouteClick() {
        val routeId = screenState.value.routeAwaitingName ?: return
        // Close the dialog first, so the screen stops showing the route before it's deleted.
        screenState.update { screen -> screen.withNameRouteDialogClosed() }
        viewModelScope.launch {
            deleteRouteWithPhotoMetadataUseCase(routeId = routeId)
        }
    }

    fun hideSnackbarRouteSavedConfirmation() {
        screenState.update { screen -> screen.copy(shouldShowSnackbarRouteSavedConfirmation = false) }
    }

    // The tracked route, or else the stopped route that waits for a name.
    private fun routeOnScreen(tracking: TrackingState, screen: ScreenState): RouteId? =
        when (tracking) {
            is TrackingState.Tracking -> tracking.routeId
            TrackingState.Idle -> screen.routeAwaitingName
        }

    private fun toState(tracking: TrackingState, screen: ScreenState, routePhotos: RoutePhotos?) =
        State(
            isTracking = tracking is TrackingState.Tracking,
            // The photos of a route that's no longer on screen are cleared in the same update that removes it,
            // e.g. when the name dialog closes, not one update later.
            photos = routePhotos?.takeIf { it.routeId == routeOnScreen(tracking, screen) }?.photos
                ?: persistentListOf(),
            shouldShowPlaceFilterBottomSheet = screen.shouldShowPlaceFilterBottomSheet,
            routeNameInput = screen.routeNameInput,
            shouldShowNameRouteDialog = screen.routeAwaitingName != null,
            shouldShowSnackbarRouteSavedConfirmation = screen.shouldShowSnackbarRouteSavedConfirmation,
        )

    private data class ScreenState(
        // Set by Stop. The name dialog is open while it's set.
        val routeAwaitingName: RouteId? = null,
        val routeNameInput: String = "",
        val shouldShowPlaceFilterBottomSheet: Boolean = false,
        val shouldShowSnackbarRouteSavedConfirmation: Boolean = false,
    ) {
        fun withNameRouteDialogClosed() = copy(routeAwaitingName = null, routeNameInput = "")
    }

    private data class RoutePhotos(val routeId: RouteId, val photos: PersistentList<PhotoMetadata>)

    data class State(
        val isTracking: Boolean = false,
        val photos: PersistentList<PhotoMetadata> = persistentListOf(),
        val shouldShowPlaceFilterBottomSheet: Boolean = false,
        val routeNameInput: String = "",
        val shouldShowNameRouteDialog: Boolean = false,
        val shouldShowSnackbarRouteSavedConfirmation: Boolean = false
    ) {

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
