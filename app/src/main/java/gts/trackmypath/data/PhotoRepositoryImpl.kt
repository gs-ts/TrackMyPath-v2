package gts.trackmypath.data

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import gts.trackmypath.data.database.photo.PhotoMetadataDao
import gts.trackmypath.data.database.photo.PhotoMetadataEntity
import gts.trackmypath.data.database.photo.toDomain
import gts.trackmypath.data.network.GooglePlacesClient
import gts.trackmypath.di.IoDispatcher
import gts.trackmypath.domain.PhotoMetadata
import gts.trackmypath.domain.PhotoRepository
import gts.trackmypath.domain.RouteId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class PhotoRepositoryImpl @Inject constructor(
    private val googlePlacesClient: GooglePlacesClient,
    private val photoMetadataDao: PhotoMetadataDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : PhotoRepository {

    override suspend fun fetchPhotoMetadataForLocation(
        routeId: RouteId,
        location: PhotoMetadata.Location
    ): Result<Unit> {
        return withContext(ioDispatcher) {

            val places = googlePlacesClient.searchNearbyPlaces(
                latLng = LatLng(location.latitude, location.longitude)
            )

            if (places.isNotEmpty()) {
                val firstPlace = places.first()
                Log.d("PhotoRepository", "firstPlace id: ${firstPlace.id}")
                Log.d("PhotoRepository", "firstPlace displayName: ${firstPlace.displayName}")
                Log.d("PhotoRepository", "firstPlace googleMapsUri: ${firstPlace.googleMapsUri}")
                Log.d(
                    "PhotoRepository",
                    "firstPlace generativeSummary: ${firstPlace.generativeSummary}"
                )
                Log.d(
                    "PhotoRepository",
                    "firstPlace neighborhoodSummary: ${firstPlace.neighborhoodSummary}"
                )

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
                        } ?: Result.failure(Exception("No photoUri available"))
                    } ?: Result.failure(Exception("No photoMetadata available"))
                } ?: Result.failure(Exception("No first place available"))
            } else {
                Log.e("GooglePlacesClient", "No places available")
                // TODO improve exceptions
                Result.failure(Exception("No places available"))
            }
        }
    }

    override fun observePhotos(): Flow<List<PhotoMetadata>> {
        return photoMetadataDao.observeAllPhotos()
            .map { entities ->
                entities.map { entity -> entity.toDomain() }
            }
            // This ensures the Flow only emits if the list content *actually* differs
            // from the previous emission, preventing unnecessary UI redraws.
            .distinctUntilChanged()
    }
}
