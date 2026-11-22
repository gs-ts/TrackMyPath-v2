package gts.trackmypath.data

import app.cash.turbine.test
import gts.trackmypath.data.database.photometadata.PhotoMetadataEntity
import gts.trackmypath.data.database.route.RouteDao
import gts.trackmypath.data.database.route.RouteEntity
import gts.trackmypath.data.repository.RouteRepositoryImpl
import gts.trackmypath.domain.route.RouteId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class RouteRepositoryTest {

    private val testScope = TestScope()
    private lateinit var routeDao: RouteDaoFake
    private lateinit var routeRepository: RouteRepositoryImpl

    @BeforeTest
    fun setup() {
        routeDao = RouteDaoFake()
        // The test scope stands in for the application scope, so runTest runs its work and waits for it.
        routeRepository = RouteRepositoryImpl(routeDao, applicationScope = testScope)
    }

    @Test
    fun `startRoute inserts a pending route and returns RouteId`() = testScope.runTest {
        val routeId = routeRepository.startRoute()

        assertEquals(expected = 123L, actual = routeId.id)
        assertEquals(expected = null, actual = routeDao.insertedRoute?.displayName)
        assertEquals(expected = emptyMap(), actual = routeDao.insertedRoute?.metadata)
    }

    @Test
    fun `finishRoute saves the finish time and metadata`() = testScope.runTest {
        val metadata = mapOf("distance" to "10km")
        routeRepository.finishRoute(RouteId(1L), finishedAt = Instant.fromEpochMilliseconds(5_000L), metadata)

        assertEquals(expected = 1L, actual = routeDao.finishedRouteId)
        assertEquals(expected = 5_000L, actual = routeDao.finishedAt)
        assertEquals(expected = metadata, actual = routeDao.finishedMetadata)
    }

    @Test
    fun `finishRoute completes even if the caller is cancelled`() = testScope.runTest {
        // For example, the user leaves the screen right after tapping Stop, which cancels the ViewModel's scope.
        val caller = launch {
            routeRepository.finishRoute(RouteId(1L), finishedAt = Instant.fromEpochMilliseconds(5_000L), emptyMap())
        }
        @OptIn(ExperimentalCoroutinesApi::class)
        runCurrent() // the write has started and is still running in the DAO
        caller.cancel()
        @OptIn(ExperimentalCoroutinesApi::class)
        advanceUntilIdle()

        assertEquals(expected = 1L, actual = routeDao.finishedRouteId)
    }

    @Test
    fun `renameRoute updates route display name`() = testScope.runTest {
        routeRepository.renameRoute(RouteId(1L), name = "New Name")

        assertEquals(expected = 1L, actual = routeDao.updatedRouteId)
        assertEquals(expected = "New Name", actual = routeDao.updatedDisplayName)
    }

    @Test
    fun `deleteRoute removes route by id`() = testScope.runTest {
        routeRepository.deleteRoute(RouteId(1L))

        assertEquals(expected = 1L, actual = routeDao.deletedRouteId)
    }

    @Test
    fun `observeRouteWithPhotoMetadataById filters empty maps and applies distinctUntilChanged`() =
        testScope.runTest {
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
    fun `observeRoutesWithPhotoMetadata maps list of routes correctly`() = testScope.runTest {
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
    var finishedAt: Long? = null
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
        finishedAt: Long,
        metadata: Map<String, String>
    ) {
        delay(WRITE_DURATION_MILLIS.milliseconds) // a real write takes time, so a test can cancel the caller while it runs
        finishedRouteId = routeId
        this.finishedAt = finishedAt
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

    private companion object {
        const val WRITE_DURATION_MILLIS = 100L
    }
}
