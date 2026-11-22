package gts.trackmypath.ui.activepath

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
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

    // Only user actions change the popups.
    // The tracking state comes from routeTracker, and the photos from the database.
    private val popups = MutableStateFlow(Popups())

    // The name typed in the name dialog. The text field edits it directly, and Confirm reads it.
    val routeName = TextFieldState()

    // The photos of the route on screen. The database query restarts only when that route changes,
    // not on every popup change (e.g. opening the filter sheet).
    @OptIn(ExperimentalCoroutinesApi::class)
    private val routePhotos: Flow<RoutePhotos?> = combine(
        routeTracker.state,
        popups,
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
        popups,
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
            initialValue = toState(routeTracker.state.value, popups.value, routePhotos = null),
        )

    // True from a tap on Start until the new route is created and handed to the tracker.
    private var isStartingRoute = false

    fun onAction(action: Action) {
        when (action) {
            Action.OnStartTrackPathClick -> onStartTrackPathClick()
            Action.OnStopTrackPathClick -> onStopTrackPathClick()
            Action.OnPlaceFiltersClick -> onPlaceFilterClick()
            Action.OnClosePlaceFilterBottomSheet -> onClosePlaceFilterBottomSheet()
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
        if (popups.value.routeAwaitingName != null) return
        val routeId = when (val tracking = routeTracker.state.value) {
            is TrackingState.Tracking -> tracking.routeId
            TrackingState.Idle -> return
        }
        popups.update { popups -> popups.copy(routeAwaitingName = routeId) }
        viewModelScope.launch {
            stopTrackingUseCase(routeId = routeId)
        }
    }

    fun onPlaceFilterClick() {
        popups.update { popups -> popups.copy(shouldShowPlaceFilterBottomSheet = true) }
    }

    fun onClosePlaceFilterBottomSheet() {
        popups.update { popups -> popups.copy(shouldShowPlaceFilterBottomSheet = false) }
    }

    fun onConfirmNameRouteDialogClick() {
        // The Confirm button is enabled only for a valid name, and the dialog is shown only for a route.
        val name = normalizeRouteName(routeName.text.toString()) ?: return
        val routeId = popups.value.routeAwaitingName ?: return
        popups.update { popups ->
            popups.copy(routeAwaitingName = null, shouldShowSnackbarRouteSavedConfirmation = true)
        }
        routeName.clearText()
        viewModelScope.launch {
            renameRouteUseCase(routeId = routeId, name = name)
        }
    }

    private fun onDiscardRouteClick() {
        val routeId = popups.value.routeAwaitingName ?: return
        // Close the dialog first, so the screen stops showing the route before it's deleted.
        popups.update { popups -> popups.copy(routeAwaitingName = null) }
        routeName.clearText()
        viewModelScope.launch {
            deleteRouteWithPhotoMetadataUseCase(routeId = routeId)
        }
    }

    fun hideSnackbarRouteSavedConfirmation() {
        popups.update { popups -> popups.copy(shouldShowSnackbarRouteSavedConfirmation = false) }
    }

    // The tracked route, or else the stopped route that waits for a name.
    private fun routeOnScreen(tracking: TrackingState, popups: Popups): RouteId? =
        when (tracking) {
            is TrackingState.Tracking -> tracking.routeId
            TrackingState.Idle -> popups.routeAwaitingName
        }

    private fun toState(tracking: TrackingState, popups: Popups, routePhotos: RoutePhotos?) =
        State(
            isTracking = tracking is TrackingState.Tracking,
            // The photos of a route that's no longer on screen are cleared in the same update that removes it,
            // e.g. when the name dialog closes, not one update later.
            photos = routePhotos?.takeIf { it.routeId == routeOnScreen(tracking, popups) }?.photos
                ?: persistentListOf(),
            popups = popups,
        )

    private data class RoutePhotos(val routeId: RouteId, val photos: PersistentList<PhotoMetadata>)

    data class State(
        val isTracking: Boolean = false,
        val photos: PersistentList<PhotoMetadata> = persistentListOf(),
        val popups: Popups = Popups(),
    ) {

        val isStreamLoading: Boolean
            get() = isTracking && photos.isEmpty()
    }

    // What pops up over the photo stream: the name dialog, the filter sheet and the "route saved" snackbar.
    data class Popups(
        // Set by Stop: the stopped route. The name dialog is open while it's set.
        val routeAwaitingName: RouteId? = null,
        val shouldShowPlaceFilterBottomSheet: Boolean = false,
        val shouldShowSnackbarRouteSavedConfirmation: Boolean = false,
    ) {

        val shouldShowNameRouteDialog: Boolean
            get() = routeAwaitingName != null
    }

    sealed interface Action {
        data object OnStartTrackPathClick : Action

        data object OnStopTrackPathClick : Action

        data object OnPlaceFiltersClick : Action

        data object OnClosePlaceFilterBottomSheet : Action

        data object OnConfirmNameRouteDialogClick : Action

        data object OnDiscardRouteClick : Action

        data object HideSnackbarRouteSavedConfirmation : Action
    }
}
