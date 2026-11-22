package gts.trackmypath.data.repository

import android.util.Log
import gts.trackmypath.data.database.photometadata.toDomain
import gts.trackmypath.data.database.route.RouteDao
import gts.trackmypath.data.database.route.RouteEntity
import gts.trackmypath.di.ApplicationScope
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.RouteRepository
import gts.trackmypath.domain.route.RouteWithPhotoMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import kotlin.time.Instant

class RouteRepositoryImpl @Inject constructor(
    private val routeDao: RouteDao,
    @param:ApplicationScope private val applicationScope: CoroutineScope
) : RouteRepository {

    override suspend fun startRoute(): RouteId {
        // Create an empty "in-progress" route.
        // finishedAt and displayName are null, metadata is empty.
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
        finishedAt: Instant,
        metadata: Map<String, String>
    ) {
        Log.d("RouteRepository", "finishRoute with routeId ${routeId.id}")
        // Runs in the application scope, so it completes even if the caller is cancelled, for example when
        // the user leaves the screen right after tapping Stop. Otherwise the route would stay unfinished and
        // be missing from Past Routes until the next launch recovers it.
        applicationScope.async {
            routeDao.finishRouteById(
                routeId = routeId.id,
                finishedAt = finishedAt.toEpochMilliseconds(),
                metadata = metadata
            )
        }.await()
    }

    override suspend fun renameRoute(
        routeId: RouteId,
        name: String
    ) {
        Log.d("RouteRepository", "renameRoute with routeId ${routeId.id} and name $name")
        routeDao.updateRouteName(routeId = routeId.id, newDisplayName = name)
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
                    createdAt = Instant.fromEpochMilliseconds(routeEntity.createdAt),
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
                        createdAt = Instant.fromEpochMilliseconds(routeEntity.createdAt),
                        metadata = routeEntity.metadata,
                        photoMetadata = photoEntities.map { photoEntity ->
                            photoEntity.toDomain()
                        }
                    )
                }
            }
    }
}
