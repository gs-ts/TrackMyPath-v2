package gts.trackmypath.ui.pastroutes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import gts.trackmypath.domain.route.ObserveRouteWithPhotoMetadataContract
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.ui.PastRouteDetailRoute
import gts.trackmypath.ui.model.RouteWithPhotoMetadataUiState
import gts.trackmypath.ui.model.toUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = PastRouteDetailViewModel.Factory::class)
class PastRouteDetailViewModel @AssistedInject constructor(
    @Assisted val navKey: PastRouteDetailRoute,
    observeRouteWithPhotoMetadataUseCase: ObserveRouteWithPhotoMetadataContract
) : ViewModel() {

    val state: StateFlow<State> = observeRouteWithPhotoMetadataUseCase(routeId = RouteId(navKey.routeId))
        .map { routeWithPhotoMetadata ->
            State(routeWithPhotoMetadata = routeWithPhotoMetadata.toUiState())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = State()
        )

    @AssistedFactory
    interface Factory {
        fun create(navKey: PastRouteDetailRoute): PastRouteDetailViewModel
    }

    data class State(
        val routeWithPhotoMetadata: RouteWithPhotoMetadataUiState? = null
    )
}
