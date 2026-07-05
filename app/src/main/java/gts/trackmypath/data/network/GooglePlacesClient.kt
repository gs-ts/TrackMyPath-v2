package gts.trackmypath.data.network

import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.PlacesStatusCodes
import com.google.android.libraries.places.api.net.kotlin.awaitFetchResolvedPhotoUri
import com.google.android.libraries.places.api.net.kotlin.awaitSearchNearby
import gts.trackmypath.di.IoDispatcher
import gts.trackmypath.domain.filters.FilterPreferencesDataStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject

interface GooglePlacesClient {

    suspend fun searchNearbyPlaces(latLng: LatLng): List<Place>

    suspend fun fetchPhotoUri(photoMetadatas: List<PhotoMetadata>): URI?
}

class GooglePlacesClientImpl @Inject constructor(
    private val placesClient: PlacesClient,
    private val filterPreferencesDataStore: FilterPreferencesDataStore,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : GooglePlacesClient {

    override suspend fun searchNearbyPlaces(latLng: LatLng): List<Place> {
        val locationRestriction = CircularBounds.newInstance(
            latLng,
            DEFAULT_RADIUS_METERS
        )

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
        } catch (apiException: ApiException) {
            Log.e("GooglePlacesClient", "Error searchNearby", apiException)
            if (apiException.statusCode == PlacesStatusCodes.INVALID_REQUEST) {
                // TODO check for unsupported types and remove them from filters
            }
            emptyList()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun fetchPhotoUri(photoMetadatas: List<PhotoMetadata>): URI? {
        if (photoMetadatas.isEmpty()) {
            return null
        }

        return try {
            withContext(ioDispatcher) {
                val photoUriResponse = placesClient.awaitFetchResolvedPhotoUri(photoMetadata = photoMetadatas[0])
                photoUriResponse.uri?.let { URI(it.toString()) }
            }
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

    companion object {
        private const val DEFAULT_RADIUS_METERS = 50.0
    }
}
