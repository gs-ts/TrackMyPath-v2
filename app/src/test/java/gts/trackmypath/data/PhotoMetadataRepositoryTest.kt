package gts.trackmypath.data

import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AuthorAttribution
import com.google.android.libraries.places.api.model.AuthorAttributions
import com.google.android.libraries.places.api.model.Place
import gts.trackmypath.data.database.photometadata.PhotoMetadataDao
import gts.trackmypath.data.database.photometadata.PhotoMetadataEntity
import gts.trackmypath.data.network.GooglePlacesClient
import gts.trackmypath.data.repository.PhotoMetadataRepositoryImpl
import gts.trackmypath.domain.PhotoMetadataUnavailableException
import gts.trackmypath.domain.PlacesUnavailableException
import gts.trackmypath.domain.PlaceIdInvalidException
import gts.trackmypath.domain.photometadata.PhotoMetadata as DomainPhotoMetadata
import gts.trackmypath.domain.route.RouteId
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.net.URI
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
            // Verify it grabbed the first one from our fake list
            assertTrue(actual = photoMetadataDao.existsForRoute(1L, "101"))
        }

    @Test
    fun `fetchPhotoMetadataForLocation returns error when searchNearbyPlaces returns empty list`() = runTest(testDispatcher) {
            val googlePlacesClient: GooglePlacesClient = GooglePlacesClientFake(withException = true)
            photoMetadataRepository = PhotoMetadataRepositoryImpl(
                googlePlacesClient = googlePlacesClient,
                photoMetadataDao = photoMetadataDao,
                ioDispatcher = testDispatcher
            )

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

    @Test
    fun `refreshAndSavePhotoUri concurrent calls only fetch from API once`() = runTest(testDispatcher) {
        val placeId = "place123"
        val expiredUri = "https://example.com/expired.jpg"
        
        // 1. Create a fake client with a deliberate delay so coroutines pile up on the Mutex
        val slowGoogleClient = object : GooglePlacesClientFake() {
            override suspend fun fetchPhotoUriByPlaceId(placeId: String): URI? {
                delay(100) // Simulate network delay
                return super.fetchPhotoUriByPlaceId(placeId)
            }
        }
        
        photoMetadataRepository = PhotoMetadataRepositoryImpl(
            googlePlacesClient = slowGoogleClient,
            photoMetadataDao = photoMetadataDao,
            ioDispatcher = testDispatcher
        )
        
        photoMetadataDao.insert(
            PhotoMetadataEntity(
                id = 1L,
                routeId = 1L,
                placeId = placeId,
                createdAt = 0L,
                displayName = "",
                location = PhotoMetadataEntity.Location(0.0, 0.0),
                photoUri = expiredUri,
                googleMapsUri = null,
                generativeSummary = null,
                neighborhoodSummary = null
            )
        )

        // 2. Launch two concurrent requests
        val deferred1 = async { photoMetadataRepository.refreshAndSavePhotoUri(placeId, expiredUri) }
        val deferred2 = async { photoMetadataRepository.refreshAndSavePhotoUri(placeId, expiredUri) }
        
        val uri1 = deferred1.await()
        val uri2 = deferred2.await()

        // 3. Verify they both got the new URI, but the API was only hit once
        assertEquals(expected = uri1, actual = uri2)
        assertEquals(expected = 1, actual = slowGoogleClient.fetchPhotoUriByPlaceIdCount)
    }

    @Test
    fun `refreshAndSavePhotoUri returns existing URI from DB if already refreshed by previous sequential call`() = runTest(testDispatcher) {
        val placeId = "place123"
        val expiredUri = "https://example.com/expired.jpg"
        val googlePlacesClient = GooglePlacesClientFake()

        photoMetadataRepository = PhotoMetadataRepositoryImpl(
            googlePlacesClient = googlePlacesClient,
            photoMetadataDao = photoMetadataDao,
            ioDispatcher = testDispatcher
        )

        // 1. Initial state: DB has the expired URI
        photoMetadataDao.insert(
            PhotoMetadataEntity(
                routeId = 1L, placeId = placeId, displayName = "", location = PhotoMetadataEntity.Location(0.0, 0.0),
                photoUri = expiredUri, googleMapsUri = null, generativeSummary = null, neighborhoodSummary = null
            )
        )

        // 2. Caller 1 realizes URI is expired and refreshes it.
        val freshUri = photoMetadataRepository.refreshAndSavePhotoUri(placeId, expiredUri)

        // 3. Caller 2 arrives LATE. It still has the old expiredUri because its UI hasn't recomposed yet.
        // It calls the repository to refresh the expiredUri.
        val lateCallerUri = photoMetadataRepository.refreshAndSavePhotoUri(placeId, expiredUri)

        // 4. Verify both callers got the same fresh URI
        assertEquals(expected = freshUri, actual = lateCallerUri)

        // 5. Verify the API was only hit EXACTLY ONCE (Caller 2 was saved by the DB check)
        assertEquals(expected = 1, actual = googlePlacesClient.fetchPhotoUriByPlaceIdCount)
    }

    @Test
    fun `refreshAndSavePhotoUri deletes from DB and returns null when place ID is no longer valid`() = runTest(testDispatcher) {
        val placeId = "invalid_place_123"
        val expiredUri = "https://example.com/expired.jpg"
        
        // Client that throws the invalid place ID exception
        val googlePlacesClient = object : GooglePlacesClientFake() {
            override suspend fun fetchPhotoUriByPlaceId(placeId: String): URI? {
                throw PlaceIdInvalidException("Place ID $placeId is no longer valid")
            }
        }

        photoMetadataRepository = PhotoMetadataRepositoryImpl(
            googlePlacesClient = googlePlacesClient,
            photoMetadataDao = photoMetadataDao,
            ioDispatcher = testDispatcher
        )

        // Initial state: DB has the expired URI
        photoMetadataDao.insert(
            PhotoMetadataEntity(
                routeId = 1L, placeId = placeId, displayName = "", location = PhotoMetadataEntity.Location(0.0, 0.0),
                photoUri = expiredUri, googleMapsUri = null, generativeSummary = null, neighborhoodSummary = null
            )
        )

        // Make sure it exists first
        assertTrue(photoMetadataDao.existsForRoute(1L, placeId))

        // Call the refresh
        val resultUri = photoMetadataRepository.refreshAndSavePhotoUri(placeId, expiredUri)

        // Verify null is returned
        assertNull(resultUri)
        
        // Verify entity was deleted from the DB
        assertTrue(!photoMetadataDao.existsForRoute(1L, placeId))
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

    override suspend fun getPhotoUriByPlaceId(placeId: String): String? {
        return savedPhotos.find { it.placeId == placeId }?.photoUri
    }

    override suspend fun updatePhotoUri(placeId: String, newUri: String) {
        val index = savedPhotos.indexOfFirst { it.placeId == placeId }
        if (index != -1) {
            val oldEntity = savedPhotos[index]
            savedPhotos[index] = oldEntity.copy(photoUri = newUri)
        }
    }

    override suspend fun deleteByPlaceId(placeId: String) {
        savedPhotos.removeAll { it.placeId == placeId }
    }
}

internal open class GooglePlacesClientFake(private val withException: Boolean = false) : GooglePlacesClient {

    var fetchPhotoUriByPlaceIdCount = 0

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

    override suspend fun fetchPhotoUriByPlaceId(placeId: String): URI? {
        fetchPhotoUriByPlaceIdCount++
        if (withException) return null
        return URI("https://example.com/refreshed_photo_$placeId.jpg")
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
