package gts.trackmypath.ui.pastroutes

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gts.trackmypath.domain.route.DeleteRouteWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.ObserveAllRoutesWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.RenameRouteUseCase
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.normalizeRouteName
import gts.trackmypath.ui.model.RouteWithPhotoMetadataUiState
import gts.trackmypath.ui.model.toUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PastRoutesViewModel @Inject constructor(
    observeAllRoutesWithPhotoMetadataUseCase: ObserveAllRoutesWithPhotoMetadataUseCase,
    private val renameRouteUseCase: RenameRouteUseCase,
    private val deleteRouteWithPhotoMetadataUseCase: DeleteRouteWithPhotoMetadataUseCase
) : ViewModel() {

    // Only user actions change the popups. The routes come from the database.
    private val popups = MutableStateFlow(Popups())

    // The name typed in the rename dialog. The text field edits it directly, and Confirm reads it.
    val routeName = TextFieldState()

    // Mapped once per database change, not on every popup change (e.g. opening a dialog),
    // so the list keeps its instance.
    private val routes: Flow<ImmutableList<RouteWithPhotoMetadataUiState>> =
        observeAllRoutesWithPhotoMetadataUseCase().map { routes -> routes.toUiState() }

    val state: StateFlow<State> = combine(routes, popups) { routes, popups ->
        State(isLoading = false, routesWithPhotoMetadata = routes, popups = popups)
    }
        .stateIn(
            scope = viewModelScope,
            // The database query stops 5 s after the screen goes away, e.g. while a route's details are open.
            // The last state stays, so coming back shows the list at once.
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = State(),
        )

    fun onAction(action: Action) {
        when (action) {
            is Action.OnRenameRouteClick -> onRenameRouteClick(action.routeId)
            Action.OnConfirmNameRouteDialogClick -> onConfirmNameRouteDialogClick()
            Action.OnDismissNameRouteDialogClick -> onDismissNameRouteDialogClick()
            is Action.OnDeleteRouteClick -> onDeleteRouteClick(action.routeId)
            Action.OnConfirmDeleteRouteClick -> onConfirmDeleteRouteClick()
            Action.OnDismissDeleteRouteDialogClick -> onDismissDeleteRouteDialogClick()
            Action.HideSnackbarRouteDeletedConfirmation -> hideSnackbarRouteDeletedConfirmation()
        }
    }

    private fun onRenameRouteClick(routeId: RouteId) {
        val currentName = state.value.routesWithPhotoMetadata.find { it.routeId == routeId }?.displayName
        routeName.setTextAndPlaceCursorAtEnd(currentName.orEmpty())
        popups.update { popups -> popups.copy(routeToRename = routeId) }
    }

    private fun onConfirmNameRouteDialogClick() {
        // The Confirm button is enabled only for a valid name.
        val name = normalizeRouteName(routeName.text.toString()) ?: return
        val routeId = popups.value.routeToRename
        if (routeId != null) {
            viewModelScope.launch {
                renameRouteUseCase(routeId = routeId, name = name)
            }
        }
        onDismissNameRouteDialogClick()
    }

    private fun onDismissNameRouteDialogClick() {
        popups.update { popups -> popups.copy(routeToRename = null) }
        routeName.clearText()
    }

    private fun onDeleteRouteClick(routeId: RouteId) {
        popups.update { popups -> popups.copy(routeIdToDelete = routeId) }
    }

    private fun onConfirmDeleteRouteClick() {
        val routeId = popups.value.routeIdToDelete ?: return
        viewModelScope.launch {
            deleteRouteWithPhotoMetadataUseCase(routeId = routeId)
            popups.update { popups ->
                popups.copy(routeIdToDelete = null, shouldShowSnackbarRouteDeletedConfirmation = true)
            }
        }
    }

    private fun hideSnackbarRouteDeletedConfirmation() {
        popups.update { popups -> popups.copy(shouldShowSnackbarRouteDeletedConfirmation = false) }
    }

    private fun onDismissDeleteRouteDialogClick() {
        popups.update { popups -> popups.copy(routeIdToDelete = null) }
    }

    data class State(
        val isLoading: Boolean = true,
        val routesWithPhotoMetadata: ImmutableList<RouteWithPhotoMetadataUiState> = persistentListOf(),
        val popups: Popups = Popups(),
    )

    // What pops up over the list: the rename dialog, the delete dialog and the "route deleted" snackbar.
    data class Popups(
        // Set by Rename in a route's menu. The rename dialog is open while it's set.
        val routeToRename: RouteId? = null,
        // Set by Delete in a route's menu. The delete dialog is open while it's set.
        val routeIdToDelete: RouteId? = null,
        val shouldShowSnackbarRouteDeletedConfirmation: Boolean = false,
    ) {

        val shouldShowRenameRouteDialog: Boolean
            get() = routeToRename != null

        val shouldShowDeletePastRouteDialog: Boolean
            get() = routeIdToDelete != null
    }

    sealed interface Action {
        data class OnRenameRouteClick(val routeId: RouteId) : Action

        data object OnConfirmNameRouteDialogClick : Action

        data object OnDismissNameRouteDialogClick : Action

        data class OnDeleteRouteClick(val routeId: RouteId) : Action

        data object OnConfirmDeleteRouteClick : Action

        data object OnDismissDeleteRouteDialogClick : Action

        data object HideSnackbarRouteDeletedConfirmation : Action
    }
}
