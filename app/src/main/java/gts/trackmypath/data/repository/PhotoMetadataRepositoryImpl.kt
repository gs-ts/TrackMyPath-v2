package gts.trackmypath.data.repository

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import gts.trackmypath.data.database.photometadata.PhotoMetadataDao
import gts.trackmypath.data.database.photometadata.PhotoMetadataEntity
import gts.trackmypath.data.network.GooglePlacesClient
import gts.trackmypath.di.IoDispatcher
import gts.trackmypath.domain.PhotoMetadataUnavailableException
import gts.trackmypath.domain.PlaceIdInvalidException
import gts.trackmypath.domain.PlacesUnavailableException
import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.domain.photometadata.PhotoMetadataRepository
import gts.trackmypath.domain.route.RouteId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val REFRESH_DEBOUNCE_WINDOW_MS = 10_000L

class PhotoMetadataRepositoryImpl @Inject constructor(
    private val googlePlacesClient: GooglePlacesClient,
    private val photoMetadataDao: PhotoMetadataDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PhotoMetadataRepository {

    private val mapMutex = Mutex()

    // Stores the active network tasks. Empty when no network calls are happening!
    private val activePhotoUriFetches = mutableMapOf<String, Deferred<String?>>()

    // Stores the timestamp (System.currentTimeMillis()) of the last successful refresh for a placeId.
    // This solves the "Late Arrival" race condition without relying on brittle URL string matching.
    private val recentRefreshes = mutableMapOf<String, Long>()

    override suspend fun fetchPhotoMetadataForLocation(
        routeId: RouteId,
        location: PhotoMetadata.Location
    ): Result<Unit> {
        Log.d("PhotoMetadataRepository", "fetchPhotoMetadataForLocation with routeId = ${routeId.id}")

        return withContext(ioDispatcher) {
            val places = googlePlacesClient.searchNearbyPlaces(
                latLng = LatLng(location.latitude, location.longitude)
            )
            if (places.isEmpty()) { // first check: no places available
                return@withContext Result.failure(exception = PlacesUnavailableException("No places available."))
            }

            // valid place is one that has photos and is not already in the DB
            val validPlace = places.firstOrNull { place ->
                val placeId = place.id ?: return@firstOrNull false

                val hasPhotos = !place.photoMetadatas.isNullOrEmpty()
                val isExisting = photoMetadataDao.existsForRoute(
                    routeId = routeId.id,
                    placeId = placeId
                )

                hasPhotos && !isExisting
            }
            if (validPlace == null) { // second check: at least one new place with photos
                return@withContext Result.failure(
                    exception = PhotoMetadataUnavailableException("No new places with photos found.")
                )
            }

            Log.d("PhotoMetadataRepository", "fetchPhotoMetadataForLocation found valid place")
            val placeId = validPlace.id!! // Safe because we filtered nulls

            // third check: fetch photoUri for the new valid place
            val photoUri = googlePlacesClient.fetchPhotoUri(photoMetadatas = validPlace.photoMetadatas ?: emptyList())
                ?: return@withContext Result.failure(
                    exception = PhotoMetadataUnavailableException("No photoUri available")
                )

            // all checks passed, insert the entity
            photoMetadataDao.insert(
                photoMetadataEntity = PhotoMetadataEntity(
                    routeId = routeId.id,
                    placeId = placeId,
                    displayName = validPlace.displayName,
                    location = PhotoMetadataEntity.Location(
                        latitude = location.latitude,
                        longitude = location.longitude
                    ),
                    photoUri = photoUri.toString(),
                    googleMapsUri = validPlace.googleMapsUri?.toString(),
                    generativeSummary = validPlace.generativeSummary?.overview,
                    neighborhoodSummary = validPlace.neighborhoodSummary?.overview?.content
                )
            )

            Result.success(value = Unit)
        }
    }

    /**
     * Refreshes an expired Google Places photo URI and saves it to the database.
     *
     * We use a [Mutex] (mapMutex) and an In-Flight map (activeFetches) alongside a database check
     * to solve two race conditions and completely eliminate memory leaks:
     *
     * 1. **Concurrent Calls:** When multiple UI components (e.g., a list item and a header) or when
     *    the user returns back the same place attempt to load the **same** expired image at the
     *    exact same time, we avoid redundant network calls.
     *
     *    **The In-Flight Map:** The repository maintains a temporary "In-Flight" map of active
     *    network tasks, keyed by `placeId`.
     *
     *    **Request Sharing:** If a refresh for `placeId: "EiffelTower"` is already in progress,
     *    any new requests for that same ID will not start a new network call.
     *    Instead, they "hitch a ride" on the existing task and wait for its result.
     *
     *    **Result:** This ensures that no matter how many times the same image is requested simultaneously,
     *    the Google Places API is only hit **exactly once**.
     *
     * 2. **Late Arrivals:** If a UI component fails to load an image a split-second *after* a refresh
     *    has already completed (due to stale UI state), we check an in-memory `recentRefreshes` cache.
     *    If the image was refreshed very recently (e.g., within the last 10 seconds), we skip the
     *    network call and return the newly saved URI from the database.
     *
     * 3. **Obsolete Place IDs:** Place IDs can become invalid over time (e.g. business closed). If the
     *    Places API returns a `NOT_FOUND` error, a [PlaceIdInvalidException] is thrown. The repository
     *    catches this exception, removes the obsolete entity from the database, and returns null to
     *    break the infinite refresh loop.
     */
    @Suppress("DeferredResultUnused")
    override suspend fun refreshAndSavePhotoUri(
        placeId: String,
        expiredPhotoUri: String
    ): String? = coroutineScope {
        Log.d("PhotoMetadataRepository", "refreshAndSavePhotoUri for placeId = $placeId")

        val currentTime = System.currentTimeMillis()

        // 1. Late Arrival Check (Debounce mechanism)
        // If we successfully refreshed this exact placeId in the last 10 seconds, this is almost
        // certainly a late arrival from a UI component holding stale state.
        // We skip the API call and return what is in the database.
        // Lazy cleanup of old entries to prevent memory leaks over long app sessions.
        // Wrapped in Mutex because we are modifying a shared mutable map concurrently.
        mapMutex.withLock {
            recentRefreshes.entries.removeAll { currentTime - it.value >= REFRESH_DEBOUNCE_WINDOW_MS }
        }

        val lastRefreshTime = mapMutex.withLock { recentRefreshes[placeId] } ?: 0L
        if (currentTime - lastRefreshTime < REFRESH_DEBOUNCE_WINDOW_MS) {
            Log.d("PhotoMetadataRepository", "refreshAndSavePhotoUri skipping API call, recently refreshed: $placeId")
            return@coroutineScope photoMetadataDao.getPhotoUriByPlaceId(placeId)
        }

        // 2. Get the existing network request, or start a new one (Solves the Concurrent Call race condition)
        val deferred = mapMutex.withLock {
            activePhotoUriFetches.getOrPut(key = placeId) {
                // .getOrPut: Get when the map has already the same placeId
                async {
                    Log.d(
                        "PhotoMetadataRepository",
                        "refreshAndSavePhotoUri start photo uri refresh for placeId = $placeId"
                    )
                    // Start the network call
                    try {
                        val freshPhotoUri = googlePlacesClient.fetchPhotoUriByPlaceId(placeId = placeId)?.toString()
                        if (freshPhotoUri != null) {
                            photoMetadataDao.updatePhotoUri(
                                placeId = placeId,
                                newUri = freshPhotoUri
                            )
                            // We are inside a Mutex here, so it is safe to write to the map
                            recentRefreshes[placeId] = System.currentTimeMillis()
                        }
                        freshPhotoUri
                    } catch (_: PlaceIdInvalidException) {
                        Log.w("PhotoMetadataRepository", "Place ID invalid, deleting from DB: $placeId")
                        photoMetadataDao.deleteByPlaceId(placeId)
                        null
                    } finally {
                        // IMPORTANT: Clean up the map the exact moment the API call finishes.
                        // Wrapped in NonCancellable because withLock is a suspend function,
                        // and this finally block might be executing during coroutine cancellation.
                        withContext(NonCancellable) {
                            mapMutex.withLock {
                                activePhotoUriFetches.remove(key = placeId)
                            }
                        }
                    }
                }
            }
        }

        // 3. Wait for the API call to finish (all concurrent callers wait for this same 'await')
        return@coroutineScope deferred.await()
    }
}
