package gts.trackmypath.data.network

import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.PlacesStatusCodes
import com.google.android.libraries.places.api.net.kotlin.awaitFetchPlace
import com.google.android.libraries.places.api.net.kotlin.awaitFetchResolvedPhotoUri
import com.google.android.libraries.places.api.net.kotlin.awaitSearchNearby
import gts.trackmypath.di.IoDispatcher
import gts.trackmypath.domain.PlaceIdInvalidException
import gts.trackmypath.domain.filters.FilterPreferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject

interface GooglePlacesClient {

    /**
     * Searches for nearby places within a default radius of the given [latLng],
     * filtered by the user's preferred place types.
     */
    suspend fun searchNearbyPlaces(latLng: LatLng): List<Place>

    /**
     * Fetches a signed, temporary photo URI for the first photo in the provided list.
     * Note: The returned URI is highly ephemeral and will expire.
     */
    suspend fun fetchPhotoUri(photoMetadatas: List<PhotoMetadata>): URI?

    /**
     * Fetches the place details for [placeId] and resolves a fresh, signed photo URI
     * for the first photo available.
     */
    suspend fun fetchPhotoUriByPlaceId(placeId: String): URI?
}

class GooglePlacesClientImpl @Inject constructor(
    private val placesClient: PlacesClient,
    private val filterPreferencesDataStore: FilterPreferencesDataStore,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : GooglePlacesClient {

    override suspend fun searchNearbyPlaces(latLng: LatLng): List<Place> {
        val locationRestriction = CircularBounds.newInstance(latLng, DEFAULT_RADIUS_METERS)

        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.PHOTO_METADATAS,
            Place.Field.GOOGLE_MAPS_URI,
            Place.Field.GENERATIVE_SUMMARY,
            Place.Field.NEIGHBORHOOD_SUMMARY,
            Place.Field.EDITORIAL_SUMMARY
        )

        val placeFilters = filterPreferencesDataStore.placeFilters.first()
        val includedTypes = placeFilters.flatMap { placeFilter ->
            placeFilter.types
        }

        return try {
            withContext(context = ioDispatcher) {
                return@withContext placesClient.awaitSearchNearby(
                    locationRestriction = locationRestriction,
                    placeFields = placeFields
                ) {
                    setIncludedTypes(includedTypes)
                }.places
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (apiException: ApiException) {
            Log.e("GooglePlacesClient", "Error searchNearby", apiException)
            if (apiException.statusCode == PlacesStatusCodes.INVALID_REQUEST) {
                // TODO check for unsupported types and remove them from filters
            }
            emptyList()
        }
    }

    /**
     * **Important Note:**
     * The URI returned by [awaitFetchResolvedPhotoUri] is a temporary, signed URL hosted by Google.
     * It is not permanent!
     *
     * While the exact expiration time isn't strictly documented and can change,
     * these URIs typically expire within a few days or even hours. After expiration,
     * attempting to load the URI will result in an HTTP 403 (Forbidden) or 404 (Not Found) error.
     */
    @Suppress("TooGenericExceptionCaught")
    override suspend fun fetchPhotoUri(photoMetadatas: List<PhotoMetadata>): URI? {
        if (photoMetadatas.isEmpty()) return null

        return try {
            withContext(ioDispatcher) {
                val photoUriResponse = placesClient.awaitFetchResolvedPhotoUri(photoMetadata = photoMetadatas[0])
                photoUriResponse.uri?.let { URI(it.toString()) }
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (apiException: ApiException) {
            Log.e("GooglePlacesClient", "Api error", apiException)
            null
        } catch (uriSyntaxException: URISyntaxException) {
            Log.e("GooglePlacesClient", "Uri syntax error", uriSyntaxException)
            null
        } catch (exception: Exception) {
            Log.e("GooglePlacesClient", "Other error", exception)
            null
        }
    }

    override suspend fun fetchPhotoUriByPlaceId(placeId: String): URI? {
        return try {
            withContext(ioDispatcher) {
                val placeFields = listOf(Place.Field.PHOTO_METADATAS)

                val response = placesClient.awaitFetchPlace(placeId, placeFields)
                val place = response.place

                fetchPhotoUri(photoMetadatas = place.photoMetadatas ?: emptyList())
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (apiException: ApiException) {
            Log.e("GooglePlacesClient", "Error fetching place with placeId = $placeId", apiException)
            if (apiException.statusCode == PlacesStatusCodes.NOT_FOUND) {
                throw PlaceIdInvalidException("Place ID $placeId is no longer valid")
            }
            null
        }
    }

    companion object {
        private const val DEFAULT_RADIUS_METERS = 50.0
    }
}
