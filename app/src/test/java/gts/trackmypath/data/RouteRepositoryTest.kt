package gts.trackmypath.data

import app.cash.turbine.test
import gts.trackmypath.data.database.photometadata.PhotoMetadataEntity
import gts.trackmypath.data.database.route.RouteDao
import gts.trackmypath.data.database.route.RouteEntity
import gts.trackmypath.domain.route.RouteId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteRepositoryTest {

    private lateinit var routeDao: RouteDaoFake
    private lateinit var routeRepository: RouteRepositoryImpl

    @BeforeTest
    fun setup() {
        routeDao = RouteDaoFake()
        routeRepository = RouteRepositoryImpl(routeDao)
    }

    @Test
    fun `observeRouteWithPhotoMetadataById filters empty maps and applies distinctUntilChanged`() =
        runTest {
            val routeId = RouteId(1L)
            val routeEntity = RouteEntity(routeId = 1L, displayName = "Test Route")

            val photoNew = PhotoMetadataEntity(
                id = 101L,
                routeId = 1L,
                placeId = "p2",
                createdAt = 5000L,
                displayName = null,
                location = PhotoMetadataEntity.Location(0.0, 0.0),
                photoUri = "uri2",
                googleMapsUri = null,
                generativeSummary = null,
                neighborhoodSummary = null
            )
            val photoOld = PhotoMetadataEntity(
                id = 100L,
                routeId = 1L,
                placeId = "p1",
                createdAt = 1000L,
                displayName = null,
                location = PhotoMetadataEntity.Location(0.0, 0.0),
                photoUri = "uri1",
                googleMapsUri = null,
                generativeSummary = null,
                neighborhoodSummary = null
            )

            val routeMapFromDb = mapOf(
                routeEntity to listOf(photoNew, photoOld) // Simulated DB response (already sorted via SQL)
            )

            routeRepository.observeRouteWithPhotoMetadataById(routeId).test {
                routeDao.dbStream.emit(routeMapFromDb)

                val firstEmission = awaitItem()
                assertEquals(expected = "Test Route", actual = firstEmission.displayName)

                // Verify Mapping (ensuring it matches the flow emitted by DB Fake)
                assertEquals(expected = 101L, actual = firstEmission.photoMetadata[0].id)
                assertEquals(expected = 100L, actual = firstEmission.photoMetadata[1].id)

                // emit the EXACT same entity again to test distinctUntilChanged
                routeDao.dbStream.emit(routeMapFromDb)

                // emit an empty map to test dropping empty results
                routeDao.dbStream.emit(emptyMap())

                // emit a new entity to ensure the flow is still active
                val updatedRouteEntity = routeEntity.copy(displayName = "Updated Route")
                routeDao.dbStream.emit(
                    mapOf(
                        updatedRouteEntity to emptyList()
                    )
                )

                val secondEmission = awaitItem()
                assertEquals(expected = "Updated Route", actual = secondEmission.displayName)

                // verify no other items were emitted (proving distinct and empty map drops worked)
                expectNoEvents()
            }
        }
}

internal class RouteDaoFake : RouteDao {

    // SharedFlow with replay = 1 mimics Room's behavior of emitting the latest
    // cached value immediately upon subscription, while allowing identical emissions.
    val dbStream = MutableSharedFlow<Map<RouteEntity, List<PhotoMetadataEntity>>>(replay = 1)

    override suspend fun insertRoute(route: RouteEntity): Long = 0L

    override suspend fun updateRoute(route: RouteEntity) {}

    override suspend fun getRouteById(routeId: Long): RouteEntity? = null

    override suspend fun deleteRouteById(routeId: Long) {}

    override fun observeRouteWithPhotosMap(routeId: Long): Flow<Map<RouteEntity, List<PhotoMetadataEntity>>> {
        return dbStream
    }

    override fun observeAllRoutesWithPhotos(): Flow<Map<RouteEntity, List<PhotoMetadataEntity>>> {
        return emptyFlow()
    }
}
