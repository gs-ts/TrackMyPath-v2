package gts.trackmypath.data

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import gts.trackmypath.data.database.photometadata.PhotoMetadataDao
import gts.trackmypath.data.database.photometadata.PhotoMetadataEntity
import gts.trackmypath.data.network.GooglePlacesClient
import gts.trackmypath.di.IoDispatcher
import gts.trackmypath.domain.PhotoMetadataUnavailableException
import gts.trackmypath.domain.PlacesUnavailableException
import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.domain.photometadata.PhotoMetadataRepository
import gts.trackmypath.domain.route.RouteId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class PhotoMetadataRepositoryImpl @Inject constructor(
    private val googlePlacesClient: GooglePlacesClient,
    private val photoMetadataDao: PhotoMetadataDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PhotoMetadataRepository {

    override suspend fun fetchPhotoMetadataForLocation(
        routeId: RouteId,
        location: PhotoMetadata.Location
    ): Result<Unit> {
        Log.d("PhotoMetadataRepository", "fetchPhotoMetadataForLocation for route ${routeId.id} and location $location")

        return withContext(ioDispatcher) {
            val places = googlePlacesClient.searchNearbyPlaces(
                latLng = LatLng(location.latitude, location.longitude)
            )

            if (places.isNotEmpty()) {
                val firstPlace = places.first()
                Log.d("PhotoMetadataRepository", "fetchPhotoMetadataForLocation found first place $firstPlace")

                firstPlace.id?.let { placeId ->
                    val photoMetadata = firstPlace.photoMetadatas

                    photoMetadata?.let {
                        val photoUri = googlePlacesClient.fetchPhotoUri(
                            photoMetadatas = firstPlace.photoMetadatas
                        )
                        photoUri?.let {
                            photoMetadataDao.insert(
                                photoMetadataEntity = PhotoMetadataEntity(
                                    routeId = routeId.id,
                                    placeId = placeId,
                                    displayName = firstPlace.displayName,
                                    location = PhotoMetadataEntity.Location(
                                        latitude = location.latitude,
                                        longitude = location.longitude
                                    ),
                                    photoUri = photoUri.toString(),
                                    googleMapsUri = firstPlace.googleMapsUri?.toString(),
                                    generativeSummary = firstPlace.generativeSummary?.overview,
                                    neighborhoodSummary = firstPlace.neighborhoodSummary?.overview?.content
                                )
                            )
                            Result.success(Unit)
                        } ?: Result.failure(exception = PhotoMetadataUnavailableException("No photoUri available"))
                    } ?: Result.failure(exception = PhotoMetadataUnavailableException("No photoMetadata available"))
                } ?: Result.failure(exception = PlacesUnavailableException("No first place available"))
            } else {
                Log.e("GooglePlacesClient", "No places available")
                Result.failure(exception = PlacesUnavailableException())
            }
        }
    }
}
