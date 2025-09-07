package gts.trackmypath.data

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import gts.trackmypath.domain.Photo
import gts.trackmypath.domain.PhotoRepository
import javax.inject.Inject

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class PhotoRepositoryImpl @Inject constructor(
    private val googlePlacesClient: GooglePlacesClient
) : PhotoRepository {

    override suspend fun fetchPhotoForLocation(latLng: LatLng): Photo? {
        val places = googlePlacesClient.searchNearby(latLng)

        return if (places.isNotEmpty()) {
            val firstPlace = places.first()
            Log.d("PhotoRepository", "firstPlace received: ${firstPlace.id}")
            firstPlace.id?.let { placeId ->
                val bitmap = googlePlacesClient.fetchPhoto(
                    photoMetadatas = firstPlace.photoMetadatas
                )

                bitmap?.let {
                    Photo(
                        id = placeId,
                        bitmap = bitmap
                    )
                }
            }
        } else {
            Log.e("GooglePlacesClient", "No places available")
            null
        }
    }
}
