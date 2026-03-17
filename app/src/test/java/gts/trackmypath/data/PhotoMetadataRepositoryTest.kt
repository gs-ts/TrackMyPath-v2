package gts.trackmypath.data

import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AuthorAttribution
import com.google.android.libraries.places.api.model.AuthorAttributions
import com.google.android.libraries.places.api.model.Place
import gts.trackmypath.data.database.photometadata.PhotoMetadataDao
import gts.trackmypath.data.database.photometadata.PhotoMetadataEntity
import gts.trackmypath.data.network.GooglePlacesClient
import gts.trackmypath.domain.PhotoMetadataUnavailableException
import gts.trackmypath.domain.PlacesUnavailableException
import gts.trackmypath.domain.photometadata.PhotoMetadata as DomainPhotoMetadata
import gts.trackmypath.domain.route.RouteId
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.net.URI
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import com.google.android.libraries.places.api.model.PhotoMetadata as PlacesPhotoMetadata

class PhotoMetadataRepositoryTest {

    private lateinit var photoMetadataDao: PhotoMetadataDao
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var photoMetadataRepository: PhotoMetadataRepositoryImpl

    @BeforeTest
    fun setup() {
        photoMetadataDao = PhotoMetadataDaoFake()
    }

    @Test
    fun `fetchPhotoMetadataForLocation returns success when searchNearbyPlaces call and fetchPhotoUri are successful`() = runTest(testDispatcher) {
            val googlePlacesClient: GooglePlacesClient = GooglePlacesClientFake()
            photoMetadataRepository =
                PhotoMetadataRepositoryImpl(googlePlacesClient, photoMetadataDao, testDispatcher)

            val result = photoMetadataRepository.fetchPhotoMetadataForLocation(
                routeId = RouteId(id = 1),
                location = DomainPhotoMetadata.Location(latitude = 0.0, longitude = 0.0)
            )

            assertTrue(actual = result.isSuccess)
            // Verify it grabbed the first one from our fake list
            assertTrue(actual = photoMetadataDao.existsForRoute(1L, "101"))
        }

    @Test
    fun `fetchPhotoMetadataForLocation returns error when searchNearbyPlaces returns empty list`() = runTest(testDispatcher) {
            val googlePlacesClient: GooglePlacesClient =
                GooglePlacesClientFake(withException = true)
            photoMetadataRepository =
                PhotoMetadataRepositoryImpl(googlePlacesClient, photoMetadataDao, testDispatcher)

            val result = photoMetadataRepository.fetchPhotoMetadataForLocation(
                routeId = RouteId(id = 1),
                location = DomainPhotoMetadata.Location(latitude = 0.0, longitude = 0.0)
            )

            assertTrue(actual = result.isFailure)
            result.exceptionOrNull()?.let { exception ->
                assertTrue(actual = exception is PlacesUnavailableException)
            }
        }

    @Test
    fun `fetchPhotoMetadataForLocation skips already existing places and inserts the next valid one`() = runTest(testDispatcher) {
            // pre-populate the db so "Place 101" is already saved for this route
            photoMetadataDao.insert(
                PhotoMetadataEntity(
                    routeId = 1L,
                    placeId = "101",
                    displayName = "Already Saved Place",
                    location = PhotoMetadataEntity.Location(0.0, 0.0),
                    photoUri = "old_uri",
                    googleMapsUri = null,
                    generativeSummary = null,
                    neighborhoodSummary = null
                )
            )

            val googlePlacesClient: GooglePlacesClient = GooglePlacesClientFake()
            photoMetadataRepository = PhotoMetadataRepositoryImpl(
                googlePlacesClient = googlePlacesClient,
                photoMetadataDao = photoMetadataDao,
                ioDispatcher = testDispatcher
            )

            val result = photoMetadataRepository.fetchPhotoMetadataForLocation(
                routeId = RouteId(id = 1),
                location = DomainPhotoMetadata.Location(latitude = 0.0, longitude = 0.0)
            )

            assertTrue(actual = result.isSuccess)

            // verify that the repository skipped 101 and correctly inserted 102
            assertTrue(actual = photoMetadataDao.existsForRoute(routeId = 1L, placeId = "102"))
        }

    @Test
    fun `fetchPhotoMetadataForLocation returns error when all places already exist`() = runTest(testDispatcher) {
            // pre-populate BOTH places returned by the Fake Client
            photoMetadataDao.insert(
                PhotoMetadataEntity(
                    id = 1L,
                    routeId = 1L,
                    placeId = "101",
                    createdAt = 0L,
                    displayName = "",
                    location = PhotoMetadataEntity.Location(0.0, 0.0),
                    photoUri = "",
                    googleMapsUri = null,
                    generativeSummary = null,
                    neighborhoodSummary = null
                )
            )
            photoMetadataDao.insert(
                PhotoMetadataEntity(
                    id = 2L,
                    routeId = 1L,
                    placeId = "102",
                    createdAt = 0L,
                    displayName = "",
                    location = PhotoMetadataEntity.Location(0.0, 0.0),
                    photoUri = "",
                    googleMapsUri = null,
                    generativeSummary = null,
                    neighborhoodSummary = null
                )
            )

            val googlePlacesClient: GooglePlacesClient = GooglePlacesClientFake()
            photoMetadataRepository =
                PhotoMetadataRepositoryImpl(googlePlacesClient, photoMetadataDao, testDispatcher)

            val result = photoMetadataRepository.fetchPhotoMetadataForLocation(
                routeId = RouteId(id = 1),
                location = DomainPhotoMetadata.Location(latitude = 0.0, longitude = 0.0)
            )

            assertTrue(actual = result.isFailure)
            assertTrue(actual = result.exceptionOrNull() is PhotoMetadataUnavailableException)
        }
}

internal class PhotoMetadataDaoFake : PhotoMetadataDao {
    // In-memory list to track state across suspension points
    private val savedPhotos = mutableListOf<PhotoMetadataEntity>()

    override suspend fun insert(photoMetadataEntity: PhotoMetadataEntity) {
        savedPhotos.add(photoMetadataEntity)
    }

    override suspend fun existsForRoute(routeId: Long, placeId: String): Boolean {
        return savedPhotos.any { it.routeId == routeId && it.placeId == placeId }
    }

    override suspend fun deleteAll() {
        savedPhotos.clear()
    }
}

internal class GooglePlacesClientFake(private val withException: Boolean = false) : GooglePlacesClient {

    override suspend fun searchNearbyPlaces(latLng: LatLng): List<Place> {
        if (withException) return emptyList()

        // Return a list of multiple places to test both the happy path and the iteration logic
        return listOf(
            createFakePlace("101", latLng),
            createFakePlace("102", latLng)
        )
    }

    override suspend fun fetchPhotoUri(photoMetadatas: List<PlacesPhotoMetadata>): URI? {
        return URI(photoMetadatas.first().authorAttributions?.asList()?.first()?.photoUri)
    }

    private fun createFakePlace(id: String, latLng: LatLng): Place {
        return Place.builder()
            .setId(id)
            .setLocation(latLng)
            .setPhotoMetadatas(
                listOf(
                    PlacesPhotoMetadata.builder("photo_ref_$id")
                        .setHeight(100)
                        .setWidth(100)
                        .setAttributions("Panathinaikos")
                        .setAuthorAttributions(
                            AuthorAttributions.newInstance(
                                listOf(
                                    AuthorAttribution.builder("Gate 13")
                                        .setUri("https://example.com")
                                        .setPhotoUri("https://example.com/photo_$id.jpg").build()
                                )
                            )
                        ).build()
                )
            ).build()
    }
}
