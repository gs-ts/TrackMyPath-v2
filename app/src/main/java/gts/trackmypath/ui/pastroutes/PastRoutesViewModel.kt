package gts.trackmypath.ui.pastroutes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gts.trackmypath.domain.route.ObserveAllRoutesWithPhotoMetadataUseCase
import gts.trackmypath.ui.model.RouteWithPhotoMetadataUiState
import gts.trackmypath.ui.model.toUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class PastRoutesViewModel @Inject constructor(
    observeAllRoutesWithPhotoMetadataUseCase: ObserveAllRoutesWithPhotoMetadataUseCase
) : ViewModel() {

    val state: StateFlow<State> = observeAllRoutesWithPhotoMetadataUseCase()
        .map { routesWithPhotoMetadata ->
            State(
                isLoading = false, // set to false once the flow emits
                routesWithPhotoMetadata = routesWithPhotoMetadata.toUiState()
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = State()
        )

    data class State(
        val isLoading: Boolean = true,
        val routesWithPhotoMetadata: ImmutableList<RouteWithPhotoMetadataUiState> = persistentListOf()
    )
}
