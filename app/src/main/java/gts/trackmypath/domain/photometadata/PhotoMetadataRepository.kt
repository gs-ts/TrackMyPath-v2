package gts.trackmypath.domain.photometadata

import gts.trackmypath.domain.route.RouteId

interface PhotoMetadataRepository {

    suspend fun fetchPhotoMetadataForLocation(
        routeId: RouteId,
        location: PhotoMetadata.Location
    ): Result<Unit>

    suspend fun refreshAndSavePhotoUri(placeId: String, expiredPhotoUri: String): String?
}
