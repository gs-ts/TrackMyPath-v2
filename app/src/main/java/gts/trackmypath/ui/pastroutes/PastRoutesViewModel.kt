package gts.trackmypath.ui.pastroutes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gts.trackmypath.domain.route.DeleteRouteWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.ObserveAllRoutesWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.ui.model.RouteWithPhotoMetadataUiState
import gts.trackmypath.ui.model.toUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PastRoutesViewModel @Inject constructor(
    observeAllRoutesWithPhotoMetadataUseCase: ObserveAllRoutesWithPhotoMetadataUseCase,
    private val deleteRouteWithPhotoMetadataUseCase: DeleteRouteWithPhotoMetadataUseCase
) : ViewModel() {

    private val routeIdToDelete = MutableStateFlow<RouteId?>(null)

    val state: StateFlow<State> = combine(
        observeAllRoutesWithPhotoMetadataUseCase(),
        routeIdToDelete
    ) { routesWithPhotoMetadata, routeIdToDelete ->
        State(
            isLoading = false,
            showDeletePastRouteDialog = routeIdToDelete != null,
            routeIdToDelete = routeIdToDelete,
            routesWithPhotoMetadata = routesWithPhotoMetadata.toUiState()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = State()
    )

    fun onDeleteRouteClick(routeId: RouteId) {
        routeIdToDelete.update { routeId }
    }

    fun onConfirmDeleteRouteClick() {
        val routeId = routeIdToDelete.value ?: return
        viewModelScope.launch {
            deleteRouteWithPhotoMetadataUseCase(routeId = routeId)
            routeIdToDelete.update { null }
        }
    }

    fun onDismissDeleteRouteDialogClick() {
        routeIdToDelete.update { null }
    }

    data class State(
        val isLoading: Boolean = true,
        val showDeletePastRouteDialog: Boolean = false,
        val routeIdToDelete: RouteId? = null,
        val routesWithPhotoMetadata: ImmutableList<RouteWithPhotoMetadataUiState> = persistentListOf()
    )
}
