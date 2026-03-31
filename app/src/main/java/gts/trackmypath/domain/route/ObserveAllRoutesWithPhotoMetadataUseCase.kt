package gts.trackmypath.domain.route

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAllRoutesWithPhotoMetadataUseCase @Inject constructor(
    private val routeRepository: RouteRepository
) {

    operator fun invoke(): Flow<List<RouteWithPhotoMetadata>> {
        return routeRepository.observeRoutesWithPhotoMetadata()
    }
}
