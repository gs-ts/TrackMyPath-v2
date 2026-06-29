package gts.trackmypath.data

import app.cash.turbine.test
import gts.trackmypath.data.database.photometadata.PhotoMetadataEntity
import gts.trackmypath.data.database.route.RouteDao
import gts.trackmypath.data.database.route.RouteEntity
import gts.trackmypath.data.repository.RouteRepositoryImpl
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
    fun `startRoute inserts a pending route and returns RouteId`() = runTest {
        val routeId = routeRepository.startRoute()

        assertEquals(expected = 123L, actual = routeId.id)
        assertEquals(expected = null, actual = routeDao.insertedRoute?.displayName)
        assertEquals(expected = emptyMap(), actual = routeDao.insertedRoute?.metadata)
    }

    @Test
    fun `finishRoute updates route with display name and metadata`() = runTest {
        val metadata = mapOf("distance" to "10km")
        routeRepository.finishRoute(RouteId(1L), "Finished Route", metadata)

        assertEquals(expected = 1L, actual = routeDao.finishedRouteId)
        assertEquals(expected = "Finished Route", actual = routeDao.finishedDisplayName)
        assertEquals(expected = metadata, actual = routeDao.finishedMetadata)
    }

    @Test
    fun `renameRoute updates route display name`() = runTest {
        routeRepository.renameRoute(RouteId(1L), "New Name")

        assertEquals(expected = 1L, actual = routeDao.updatedRouteId)
        assertEquals(expected = "New Name", actual = routeDao.updatedDisplayName)
    }

    @Test
    fun `deleteRoute removes route by id`() = runTest {
        routeRepository.deleteRoute(RouteId(1L))
    }

    @Test
    fun `observeRouteWithPhotoMetadataById filters empty maps and applies distinctUntilChanged`() =
        runTest {
            val routeId = RouteId(1L)
            val routeEntity =
                RouteEntity(routeId = 1L, displayName = "Test Route", createdAt = 1000L)

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
                routeEntity to listOf(
                    photoNew,
                    photoOld
                ) // Simulated DB response (already sorted via SQL)
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

    @Test
    fun `observeRoutesWithPhotoMetadata maps list of routes correctly`() = runTest {
        val routeEntity1 = RouteEntity(routeId = 1L, displayName = "Route 1", createdAt = 1000L)
        val routeEntity2 = RouteEntity(routeId = 2L, displayName = "Route 2", createdAt = 2000L)

        val photo1 = PhotoMetadataEntity(
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
        val photo2 = PhotoMetadataEntity(
            id = 200L,
            routeId = 2L,
            placeId = "p2",
            createdAt = 2000L,
            displayName = null,
            location = PhotoMetadataEntity.Location(0.0, 0.0),
            photoUri = "uri2",
            googleMapsUri = null,
            generativeSummary = null,
            neighborhoodSummary = null
        )

        val routeMap = mapOf(
            routeEntity1 to listOf(photo1),
            routeEntity2 to listOf(photo2)
        )

        routeRepository.observeRoutesWithPhotoMetadata().test {
            routeDao.allRoutesStream.emit(routeMap)

            val emissions = awaitItem()
            assertEquals(2, emissions.size)

            val route1 = emissions.find { it.routeId.id == 1L }
            val route2 = emissions.find { it.routeId.id == 2L }

            assertEquals(expected = "Route 1", actual = route1?.displayName)
            assertEquals(expected = "Route 2", actual = route2?.displayName)

            assertEquals(expected = 100L, actual = route1?.photoMetadata?.first()?.id)
            assertEquals(expected = 200L, actual = route2?.photoMetadata?.first()?.id)

            expectNoEvents()
        }
    }
}

internal class RouteDaoFake : RouteDao {

    val dbStream = MutableSharedFlow<Map<RouteEntity, List<PhotoMetadataEntity>>>(replay = 1)
    val allRoutesStream = MutableSharedFlow<Map<RouteEntity, List<PhotoMetadataEntity>>>(replay = 1)

    // Variables to capture DAO method inputs
    var insertedRoute: RouteEntity? = null
    var finishedRouteId: Long? = null
    var finishedDisplayName: String? = null
    var finishedMetadata: Map<String, String>? = null

    var updatedRouteId: Long? = null
    var updatedDisplayName: String? = null

    var deletedRouteId: Long? = null

    override suspend fun insertRoute(route: RouteEntity): Long {
        insertedRoute = route
        return 123L
    }

    override suspend fun finishRouteById(
        routeId: Long,
        displayName: String,
        metadata: Map<String, String>
    ) {
        finishedRouteId = routeId
        finishedDisplayName = displayName
        finishedMetadata = metadata
    }

    override suspend fun getRouteById(routeId: Long): RouteEntity? = null

    override suspend fun updateRouteName(routeId: Long, newDisplayName: String) {
        updatedRouteId = routeId
        updatedDisplayName = newDisplayName
    }

    override suspend fun deleteRouteById(routeId: Long) {
        deletedRouteId = routeId
    }

    override fun observeRouteWithPhotosMap(routeId: Long): Flow<Map<RouteEntity, List<PhotoMetadataEntity>>> {
        return dbStream
    }

    override fun observeAllRoutesWithPhotos(): Flow<Map<RouteEntity, List<PhotoMetadataEntity>>> {
        return allRoutesStream
    }
}
