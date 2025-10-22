package gts.trackmypath.data

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import gts.trackmypath.domain.PhotoMetadata
import gts.trackmypath.domain.PhotoRepository
import javax.inject.Inject

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class PhotoRepositoryImpl @Inject constructor(
    private val googlePlacesClient: GooglePlacesClient
) : PhotoRepository {

    override suspend fun fetchPhotoMetadataForLocation(latLng: LatLng): Result<PhotoMetadata> {
        val places = googlePlacesClient.searchNearbyPlaces(latLng)

        return if (places.isNotEmpty()) {
            val firstPlace = places.first()
            Log.d("PhotoRepository", "firstPlace received: ${firstPlace.id}")
            firstPlace.id?.let { placeId ->
                val photoUri = googlePlacesClient.fetchPhotoUri(
                    photoMetadatas = firstPlace.photoMetadatas
                )

                photoUri?.let {
                    Result.success(
                        PhotoMetadata(
                            id = placeId,
                            photoUri = photoUri
                        )
                    )
                } ?: Result.failure(Exception("No photo available"))
            } ?: Result.failure(Exception("No first place available"))
        } else {
            Log.e("GooglePlacesClient", "No places available")
            // TODO improve exceptions
            Result.failure(Exception("No places available"))
        }
    }
}
