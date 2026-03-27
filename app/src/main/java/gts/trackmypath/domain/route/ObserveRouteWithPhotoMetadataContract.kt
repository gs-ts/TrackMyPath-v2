package gts.trackmypath.domain.route

import kotlinx.coroutines.flow.Flow

interface ObserveRouteWithPhotoMetadataContract {
    operator fun invoke(routeId: RouteId): Flow<RouteWithPhotoMetadata>
}
