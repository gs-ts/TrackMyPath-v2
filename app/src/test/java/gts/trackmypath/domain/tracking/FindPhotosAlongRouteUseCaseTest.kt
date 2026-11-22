package gts.trackmypath.domain.tracking

import app.cash.turbine.test
import gts.trackmypath.domain.Coordinates
import gts.trackmypath.domain.LocationUnavailableException
import gts.trackmypath.domain.filters.FilterPreferencesDataStore
import gts.trackmypath.domain.filters.PlaceFilter
import gts.trackmypath.domain.photometadata.PhotoMetadataRepository
import gts.trackmypath.domain.route.RouteId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class FindPhotosAlongRouteUseCaseTest {

    private val routeId = RouteId(id = 7)
    private val firstLocation = Coordinates(latitude = 37.97, longitude = 23.72)
    private val secondLocation = Coordinates(latitude = 37.98, longitude = 23.73)

    private val locationSource = LocationSourceFake()
    private val filterPreferences = FilterPreferencesDataStoreFake(initialFilters = setOf(PlaceFilter.CULTURE))
    private val photoMetadataRepository = PhotoMetadataRepositoryFake()
    private val findPhotosAlongRoute = FindPhotosAlongRouteUseCase(
        locationSource = locationSource,
        filterPreferencesDataStore = filterPreferences,
        photoMetadataRepository = photoMetadataRepository,
    )

    @Test
    fun `searches near each new location with the filters chosen at that moment`() = runTest {
        findPhotosAlongRoute(routeId).test {
            locationSource.emit(firstLocation)
            awaitItem()
            // e.g. the user changes the filters while walking
            filterPreferences.setPlaceFilters(setOf(PlaceFilter.SHOPPING))
            locationSource.emit(secondLocation)
            awaitItem()
        }

        assertEquals(
            expected = listOf(
                Search(routeId, firstLocation, setOf(PlaceFilter.CULTURE)),
                Search(routeId, secondLocation, setOf(PlaceFilter.SHOPPING)),
            ),
            actual = photoMetadataRepository.searches,
        )
    }

    @Test
    fun `a newer location cancels the search near the previous one`() = runTest {
        val firstSearchCanFinish = CompletableDeferred<Unit>()
        photoMetadataRepository.onSearch = { search ->
            if (search.location == firstLocation) firstSearchCanFinish.await()
            Result.success(Unit)
        }

        findPhotosAlongRoute(routeId).test {
            locationSource.emit(firstLocation)
            locationSource.emit(secondLocation)
            firstSearchCanFinish.complete(Unit) // too late: the first search was cancelled

            assertTrue(actual = awaitItem().isSuccess)
            expectNoEvents()
        }
        assertEquals(
            expected = listOf(secondLocation),
            actual = photoMetadataRepository.finishedSearches.map { search -> search.location },
        )
    }

    @Test
    fun `a failed search is reported and the next location searches again`() = runTest {
        val failure = IllegalStateException("Disk full")
        photoMetadataRepository.onSearch = { search ->
            if (search.location == firstLocation) throw failure
            Result.success(Unit)
        }

        findPhotosAlongRoute(routeId).test {
            locationSource.emit(firstLocation)
            assertEquals(expected = Result.failure(failure), actual = awaitItem())

            locationSource.emit(secondLocation)
            assertEquals(expected = Result.success(Unit), actual = awaitItem())
        }
    }

    @Test
    fun `fails when location updates can't be started`() = runTest {
        findPhotosAlongRoute(routeId).test {
            // e.g. Google Play services is missing or outdated
            locationSource.fail(LocationUnavailableException(cause = IllegalStateException("No Play services")))

            // Only the type: with stack trace recovery on, the test can get a copy of the exception.
            assertIs<LocationUnavailableException>(awaitError())
        }
        assertTrue(actual = photoMetadataRepository.searches.isEmpty())
    }

    private data class Search(
        val routeId: RouteId,
        val location: Coordinates,
        val placeFilters: Set<PlaceFilter>,
    )

    // Works like LocationProvider: a failure is shared as a value and thrown to the collector.
    private class LocationSourceFake : LocationSource {
        private val updates = MutableSharedFlow<Result<Coordinates>>()

        override val locations: Flow<Coordinates> = updates.map { update -> update.getOrThrow() }

        suspend fun emit(location: Coordinates) = updates.emit(Result.success(location))

        suspend fun fail(exception: Throwable) = updates.emit(Result.failure(exception))
    }

    private class FilterPreferencesDataStoreFake(initialFilters: Set<PlaceFilter>) : FilterPreferencesDataStore {
        override val placeFilters = MutableStateFlow(initialFilters)

        override suspend fun setPlaceFilters(filters: Set<PlaceFilter>) {
            placeFilters.value = filters
        }
    }

    private class PhotoMetadataRepositoryFake : PhotoMetadataRepository {
        val searches = mutableListOf<Search>()
        val finishedSearches = mutableListOf<Search>()
        var onSearch: suspend (Search) -> Result<Unit> = { Result.success(Unit) }

        override suspend fun fetchPhotoMetadataForLocation(
            routeId: RouteId,
            location: Coordinates,
            placeFilters: Set<PlaceFilter>
        ): Result<Unit> {
            val search = Search(routeId, location, placeFilters)
            searches += search
            return onSearch(search).also { finishedSearches += search }
        }

        override suspend fun refreshAndSavePhotoUri(placeId: String, expiredPhotoUri: String): String? = null
    }
}
