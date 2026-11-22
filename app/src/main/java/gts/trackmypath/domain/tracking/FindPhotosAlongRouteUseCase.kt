package gts.trackmypath.domain.tracking

import gts.trackmypath.domain.LocationUnavailableException
import gts.trackmypath.domain.filters.FilterPreferencesDataStore
import gts.trackmypath.domain.photometadata.PhotoMetadataRepository
import gts.trackmypath.domain.route.RouteId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

/**
 * The app's main rule while a route is tracked: for each new location, find one new place photo nearby,
 * among the places that match the user's current filters, and save it to the route.
 *
 * - A newer location cancels the search for the previous one.
 * - A search that finds nothing new, or can't reach Google Places, returns a failure, and the next location
 *   tries again.
 * - The flow emits the result of each search. It fails with [LocationUnavailableException] when location
 *   updates can't be started, and with any unexpected error, such as a bug or a database failure. Then the
 *   caller stops tracking, instead of failing quietly at every location.
 */
class FindPhotosAlongRouteUseCase @Inject constructor(
    private val locationSource: LocationSource,
    private val filterPreferencesDataStore: FilterPreferencesDataStore,
    private val photoMetadataRepository: PhotoMetadataRepository,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(routeId: RouteId): Flow<Result<Unit>> =
        locationSource.locations.mapLatest { location ->
            photoMetadataRepository.fetchPhotoMetadataForLocation(
                routeId = routeId,
                location = location,
                placeFilters = filterPreferencesDataStore.placeFilters.first(),
            )
        }
}
