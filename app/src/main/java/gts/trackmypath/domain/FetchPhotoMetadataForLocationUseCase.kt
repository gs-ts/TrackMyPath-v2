package gts.trackmypath.domain

import javax.inject.Inject

class FetchPhotoMetadataForLocationUseCase @Inject constructor(private val photoRepository: PhotoRepository) {

    suspend operator fun invoke(
        routeId: RouteId,
        location: PhotoMetadata.Location
    ): Result<Unit> {
        return photoRepository.fetchPhotoMetadataForLocation(
            routeId = routeId,
            location = location
        )
    }
}
