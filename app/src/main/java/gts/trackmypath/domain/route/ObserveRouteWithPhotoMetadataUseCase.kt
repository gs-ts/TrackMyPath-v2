package gts.trackmypath.domain.route

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveRouteWithPhotoMetadataUseCase @Inject constructor(
    private val routeRepository: RouteRepository
) : ObserveRouteWithPhotoMetadataContract {

    override operator fun invoke(routeId: RouteId): Flow<RouteWithPhotoMetadata> {
        return routeRepository.observeRouteWithPhotoMetadataById(routeId = routeId)
    }
}
