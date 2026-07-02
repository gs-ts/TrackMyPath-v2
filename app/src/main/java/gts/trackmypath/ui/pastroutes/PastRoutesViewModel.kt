package gts.trackmypath.ui.pastroutes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gts.trackmypath.domain.route.DeleteRouteWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.ObserveAllRoutesWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.RenameRouteUseCase
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.ui.model.RouteWithPhotoMetadataUiState
import gts.trackmypath.ui.model.toUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PastRoutesViewModel @Inject constructor(
    private val observeAllRoutesWithPhotoMetadataUseCase: ObserveAllRoutesWithPhotoMetadataUseCase,
    private val renameRouteUseCase: RenameRouteUseCase,
    private val deleteRouteWithPhotoMetadataUseCase: DeleteRouteWithPhotoMetadataUseCase
) : ViewModel() {

    val state: StateFlow<State>
        field = MutableStateFlow(State())

    init {
        observeAllRoutesWithPhotoMetadata()
    }

    fun observeAllRoutesWithPhotoMetadata() {
        viewModelScope.launch {
            observeAllRoutesWithPhotoMetadataUseCase()
                .collect { routesWithPhotoMetadata ->
                    state.update { state ->
                        state.copy(
                            isLoading = false,
                            routesWithPhotoMetadata = routesWithPhotoMetadata.toUiState()
                        )
                    }
                }
        }
    }

    fun onRenameRouteClick(routeId: RouteId) {
        state.update { state ->
            state.copy(
                routeToRename = routeId,
                routeNameInput = state.routesWithPhotoMetadata
                    .find { it.routeId == routeId }
                    ?.displayName ?: ""
            )
        }
    }

    fun onRouteNameChange(newRouteName: String) {
        state.update { state -> state.copy(routeNameInput = newRouteName) }
    }

    fun onConfirmNameRouteDialogClick() {
        val routeId = state.value.routeToRename
        val routeName = state.value.routeNameInput
        routeId?.let {
            viewModelScope.launch {
                renameRouteUseCase(
                    routeId = routeId,
                    newDisplayName = routeName
                )
            }
        }
        onDismissNameRouteDialogClick()
    }

    fun onDismissNameRouteDialogClick() {
        state.update { state ->
            state.copy(
                routeToRename = null,
                routeNameInput = ""
            )
        }
    }

    fun onDeleteRouteClick(routeId: RouteId) {
        state.update { state ->
            state.copy(routeIdToDelete = routeId)
        }
    }

    fun onConfirmDeleteRouteClick() {
        val routeId = state.value.routeIdToDelete ?: return
        viewModelScope.launch {
            deleteRouteWithPhotoMetadataUseCase(routeId = routeId)
            state.update { state ->
                state.copy(
                    routeIdToDelete = null,
                    showSnackbarRouteDeletedConfirmation = true
                )
            }
        }
    }

    fun onHideSnackbarRouteDeletedConfirmation() {
        state.update { state ->
            state.copy(showSnackbarRouteDeletedConfirmation = false)
        }
    }

    fun onDismissDeleteRouteDialogClick() {
        state.update { state ->
            state.copy(
                routeIdToDelete = null
            )
        }
    }

    data class State(
        val isLoading: Boolean = true,
        val routesWithPhotoMetadata: ImmutableList<RouteWithPhotoMetadataUiState> = persistentListOf(),
        val routeToRename: RouteId? = null,
        val routeNameInput: String = "",
        val routeIdToDelete: RouteId? = null,
        val showSnackbarRouteDeletedConfirmation: Boolean = false
    ) {

        val showRenameRouteDialog: Boolean
            get() = routeToRename != null

        val showDeletePastRouteDialog: Boolean
            get() = routeIdToDelete != null
    }
}
