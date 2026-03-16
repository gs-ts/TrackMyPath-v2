package gts.trackmypath.domain.route

import kotlinx.coroutines.flow.Flow

interface RouteRepository {

    // called when the user clicks "Play" and returns the id of the route
    suspend fun startRoute(): RouteId

    // called when the user clicks "Stop" and adds a name and metadata if any
    suspend fun finishRoute(
        routeId: RouteId,
        displayName: String,
        metadata: Map<String, String>
    )

    suspend fun deleteRoute(routeId: RouteId)

    fun observeRouteWithPhotoMetadataById(routeId: RouteId): Flow<RouteWithPhotoMetadata>
}

@JvmInline
value class RouteId(val id: Long)
