package gts.trackmypath.domain.route

import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant

interface RouteRepository {

    // called when the user clicks "Play" and returns the id of the route
    suspend fun startRoute(): RouteId

    // called when the user clicks "Stop": the route is kept and shown in Past Routes, with or without a name.
    suspend fun finishRoute(
        routeId: RouteId,
        finishedAt: Instant,
        metadata: Map<String, String>
    )

    // called when the user confirms a name; pass it through normalizeRouteName first
    suspend fun renameRoute(routeId: RouteId, name: String)

    suspend fun deleteRoute(routeId: RouteId)

    fun observeRouteWithPhotoMetadataById(routeId: RouteId): Flow<RouteWithPhotoMetadata>

    fun observeRoutesWithPhotoMetadata(): Flow<List<RouteWithPhotoMetadata>>
}

@JvmInline
value class RouteId(val id: Long)
