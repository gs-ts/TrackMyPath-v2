package gts.trackmypath.domain.photo

import gts.trackmypath.domain.route.RouteId
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {

    suspend fun fetchPhotoMetadataForLocation(
        routeId: RouteId,
        location: PhotoMetadata.Location
    ): Result<Unit>

    fun observePhotos(): Flow<List<PhotoMetadata>>
}
