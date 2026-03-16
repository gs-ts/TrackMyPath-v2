package gts.trackmypath.domain.photometadata

import gts.trackmypath.domain.route.RouteId
import javax.inject.Inject

class FetchPhotoMetadataForLocationUseCase @Inject constructor(
    private val photoMetadataRepository: PhotoMetadataRepository
) {

    suspend operator fun invoke(
        routeId: RouteId,
        location: PhotoMetadata.Location
    ): Result<Unit> {
        return photoMetadataRepository.fetchPhotoMetadataForLocation(
            routeId = routeId,
            location = location
        )
    }
}
