package gts.trackmypath.domain.photometadata

import gts.trackmypath.domain.Coordinates
import gts.trackmypath.domain.filters.PlaceFilter
import gts.trackmypath.domain.route.RouteId

interface PhotoMetadataRepository {

    /**
     * Saves to [routeId] the photo of one place near [location] that matches [placeFilters], has a photo,
     * and isn't in the route yet.
     */
    suspend fun fetchPhotoMetadataForLocation(
        routeId: RouteId,
        location: Coordinates,
        placeFilters: Set<PlaceFilter>
    ): Result<Unit>

    suspend fun refreshAndSavePhotoUri(placeId: String, expiredPhotoUri: String): String?
}
