package gts.trackmypath.data

import android.util.Log
import gts.trackmypath.data.database.photometadata.toDomain
import gts.trackmypath.data.database.route.RouteDao
import gts.trackmypath.data.database.route.RouteEntity
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.RouteRepository
import gts.trackmypath.domain.route.RouteWithPhotoMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class RouteRepositoryImpl @Inject constructor(private val routeDao: RouteDao) : RouteRepository {

    override suspend fun startRoute(): RouteId {
        // Create an empty "in-progress" route.
        // displayName is null, metadata is empty.
        val pendingRoute = RouteEntity(
            displayName = null,
            metadata = emptyMap()
        )

        val id = routeDao.insertRoute(route = pendingRoute)

        Log.d("RouteRepository", "startRoute with routeId $id")
        return RouteId(id = id)
    }

    override suspend fun finishRoute(
        routeId: RouteId,
        displayName: String,
        metadata: Map<String, String>
    ) {
        // Fetch the pending route we created at the startRoute
        val existingRoute = routeDao.getRouteById(routeId = routeId.id)

        if (existingRoute != null) {
            // create an updated copy with the user's input
            val completedRoute = existingRoute.copy(
                displayName = displayName,
                metadata = metadata
            )

            routeDao.updateRoute(route = completedRoute)
            Log.d("RouteRepository", "finishRoute with routeId ${routeId.id} and displayName $displayName")
        }
    }

    override suspend fun deleteRoute(routeId: RouteId) {
        Log.d("RouteRepository", "deleteRoute with routeId ${routeId.id}")
        routeDao.deleteRouteById(routeId = routeId.id)
    }

    override fun observeRouteWithPhotoMetadataById(routeId: RouteId): Flow<RouteWithPhotoMetadata> {
        Log.d("RouteRepository", "observeRouteWithPhotoMetadataById with routeId ${routeId.id}")

        return routeDao.observeRouteWithPhotosMap(routeId = routeId.id)
            .mapNotNull { routeWithPhotoMetadataMap ->
                if (routeWithPhotoMetadataMap.isEmpty()) return@mapNotNull null

                // since we query by a specific routeId, the Map will only ever have exactly 1 entry.
                val entry = routeWithPhotoMetadataMap.entries.first()
                val routeEntity = entry.key
                val preSortedPhotos = entry.value

                RouteWithPhotoMetadata(
                    routeId = RouteId(id = routeEntity.routeId),
                    displayName = routeEntity.displayName,
                    metadata = routeEntity.metadata,
                    photoMetadata = preSortedPhotos.map { photoEntity ->
                        photoEntity.toDomain()
                    }
                )
            }
            .distinctUntilChanged()
    }

    override fun observeRoutesWithPhotoMetadata(): Flow<List<RouteWithPhotoMetadata>> {
        Log.d("RouteRepository", "observeRoutesWithPhotoMetadata")

        return routeDao.observeAllRoutesWithPhotos()
            .map { routeWithPhotoMetadataMap ->
                routeWithPhotoMetadataMap.map { (routeEntity, photoEntities) ->

                    RouteWithPhotoMetadata(
                        routeId = RouteId(id = routeEntity.routeId),
                        displayName = routeEntity.displayName,
                        metadata = routeEntity.metadata,
                        photoMetadata = photoEntities.map { photoEntity ->
                            photoEntity.toDomain()
                        }
                    )
                }
            }
    }
}
