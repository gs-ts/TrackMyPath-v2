package gts.trackmypath

import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AuthorAttribution
import com.google.android.libraries.places.api.model.AuthorAttributions
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import gts.trackmypath.data.network.GooglePlacesClient
import gts.trackmypath.data.PhotoRepositoryImpl
import gts.trackmypath.data.database.photo.PhotoMetadataDao
import gts.trackmypath.data.database.photo.PhotoMetadataEntity
import gts.trackmypath.domain.PlacesUnavailableException
import gts.trackmypath.domain.RouteId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import gts.trackmypath.domain.PhotoMetadata as DomainPhotoMetadata
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotoRepositoryTest {

    private val photoMetadataDao: PhotoMetadataDao = PhotoMetadataDaoFake()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var photoRepository: PhotoRepositoryImpl

    @Test
    fun `fetchPhotoMetadataForLocation returns success when searchNearbyPlaces call and fetchPhotoUri are successful`() =
        runTest(testDispatcher) {
            val googlePlacesClient: GooglePlacesClient = GooglePlacesClientFake()
            photoRepository = PhotoRepositoryImpl(googlePlacesClient, photoMetadataDao, testDispatcher)

            val result = photoRepository.fetchPhotoMetadataForLocation(
                routeId = RouteId(id = 1),
                location = DomainPhotoMetadata.Location(
                    latitude = 0.0,
                    longitude = 0.0
                )
            )

            assertTrue(actual = result.isSuccess)

            val photoMetadata = photoRepository.observePhotos().first()
            assertEquals(
                expected = "101",
                actual = photoMetadata.first().placeId
            )
        }

    @Test
    fun `fetchPhotoMetadataForLocation returns error when searchNearbyPlaces returns empty list`() =
        runTest(testDispatcher) {
            val googlePlacesClient: GooglePlacesClient = GooglePlacesClientFake(withException = true)
            photoRepository = PhotoRepositoryImpl(googlePlacesClient, photoMetadataDao, testDispatcher)

            val result = photoRepository.fetchPhotoMetadataForLocation(
                routeId = RouteId(id = 1),
                location = DomainPhotoMetadata.Location(
                    latitude = 0.0,
                    longitude = 0.0
                )
            )

            assertTrue(actual = result.isFailure)
            result.exceptionOrNull()?.let { exception ->
                assertTrue(actual = exception is PlacesUnavailableException)
            }
        }
}

internal class GooglePlacesClientFake(private val withException: Boolean = false) : GooglePlacesClient {

    override suspend fun searchNearbyPlaces(latLng: LatLng): List<Place> {

        if (withException) {
            return emptyList()
        } else {
            return listOf(
                Place.builder()
                    .setId("101")
                    .setLocation(latLng)
                    .setPhotoMetadatas(
                        listOf(
                            PhotoMetadata.builder("photo_reference_id")
                                .setHeight(100)
                                .setWidth(100)
                                .setAttributions("Panathinaikos")
                                .setAuthorAttributions(
                                    AuthorAttributions.newInstance(
                                        listOf(
                                            AuthorAttribution.builder("Gate 13")
                                                .setUri("https://example.com")
                                                .setPhotoUri("https://example.com/photo.jpg")
                                                .build()
                                        )
                                    )
                                )
                                .build()
                        )
                    )
                    .build()
            )
        }
    }

    override suspend fun fetchPhotoUri(photoMetadatas: List<PhotoMetadata>): URI? {
        return URI(photoMetadatas.first().authorAttributions?.asList()?.first()?.photoUri)
    }
}

internal class PhotoMetadataDaoFake : PhotoMetadataDao {

    override suspend fun insert(photoMetadataEntity: PhotoMetadataEntity) {}

    override fun observeAllPhotos(): Flow<List<PhotoMetadataEntity>> {
        return flowOf(
            listOf(
                PhotoMetadataEntity(
                    id = 1,
                    routeId = 1,
                    placeId = "101",
                    displayName = null,
                    location = PhotoMetadataEntity.Location(
                        latitude = 0.0,
                        longitude = 0.0,
                    ),
                    photoUri = "https://example.com/photo.jpg",
                    googleMapsUri = null,
                    generativeSummary = null,
                    neighborhoodSummary = null,
                )
            )
        )
    }

    override suspend fun getById(id: String): PhotoMetadataEntity? {
        return null
    }

    override suspend fun deleteAll() {}
}
