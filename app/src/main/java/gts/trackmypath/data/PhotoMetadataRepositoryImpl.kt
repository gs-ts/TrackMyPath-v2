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

            if (places.isEmpty()) { // first check: no places available
                Log.e("GooglePlacesClient", "No places available")
                return@withContext Result.failure(PlacesUnavailableException("No places available"))
            }

            val validPlace = places.firstOrNull { place ->
                val placeId = place.id ?: return@firstOrNull false
                val hasPhotos = !place.photoMetadatas.isNullOrEmpty()

                val alreadyExists = photoMetadataDao.existsForRoute(
                    routeId = routeId.id,
                    placeId = placeId
                )

                hasPhotos && !alreadyExists
            }

            if (validPlace == null) { // second check: at least one place with photos
                return@withContext Result.failure(PhotoMetadataUnavailableException("No new places with photos found."))
            }

            Log.d("PhotoMetadataRepository", "fetchPhotoMetadataForLocation found valid place ${validPlace.id}")
            val placeId = validPlace.id!! // Safe because we filtered nulls in the predicate

            // third check: fetch photoUri
            val photoUri = googlePlacesClient.fetchPhotoUri(photoMetadatas = validPlace.photoMetadatas)
                ?: return@withContext Result.failure(PhotoMetadataUnavailableException("No photoUri available"))

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

            Result.success(Unit)
        }
    }
}
