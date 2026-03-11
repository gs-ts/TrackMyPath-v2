package gts.trackmypath.data.network

import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.kotlin.awaitFetchResolvedPhotoUri
import com.google.android.libraries.places.api.net.kotlin.awaitSearchNearby
import gts.trackmypath.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
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
            Place.Field.NEIGHBORHOOD_SUMMARY
        )

        return try {
            withContext(context = ioDispatcher) {
                return@withContext placesClient.awaitSearchNearby(
                    locationRestriction = locationRestriction,
                    placeFields = placeFields
                ).places
            }
        } catch (apiException: ApiException) {
            Log.e("GooglePlacesClientImpl", "Error searchNearby", apiException)
            emptyList()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun fetchPhotoUri(photoMetadatas: List<PhotoMetadata>): URI? {
        if (photoMetadatas.isEmpty()) {
            Log.e("GooglePlacesClientImpl", "No photo metadata available")
            return null
        }

        return try {
            withContext(ioDispatcher) {
                val photoUriResponse = placesClient.awaitFetchResolvedPhotoUri(photoMetadata = photoMetadatas[0])
                URI(photoUriResponse.uri.toString())
            }
        } catch (apiException: ApiException) {
            Log.e("GooglePlacesClientImpl", "Error fetchPhotoUri", apiException)
            null
        } catch (uriSyntaxException: URISyntaxException) {
            Log.e("GooglePlacesClientImpl", "Uri syntax error", uriSyntaxException)
            null
        } catch (exception: Exception) {
            Log.e("GooglePlacesClientImpl", "Error fetchPhotoUri", exception)
            null
        }
    }

    companion object {
        private const val DEFAULT_RADIUS_METERS = 50.0
    }
}
