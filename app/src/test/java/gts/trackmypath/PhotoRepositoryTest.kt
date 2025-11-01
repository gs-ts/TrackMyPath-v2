package gts.trackmypath

import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AuthorAttribution
import com.google.android.libraries.places.api.model.AuthorAttributions
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import gts.trackmypath.data.GooglePlacesClient
import gts.trackmypath.data.PhotoRepositoryImpl
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhotoRepositoryTest {

    private val googlePlacesClient: GooglePlacesClient = GooglePlacesClientFake()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var photoRepository: PhotoRepositoryImpl

    @Before
    fun setUp() {
        photoRepository = PhotoRepositoryImpl(googlePlacesClient)
    }

    @Test
    fun `fetchPhotoMetadataForLocation returns success when searchNearbyPlaces call and fetchPhotoUri are successful`() =
        runTest(testDispatcher) {
            val result = photoRepository.fetchPhotoMetadataForLocation(LatLng(1.0, 1.0))

            assertTrue(actual = result.isSuccess)
            assertEquals(result.getOrNull()?.id, "101")
        }
}

internal class GooglePlacesClientFake() : GooglePlacesClient {
    override suspend fun searchNearbyPlaces(latLng: LatLng): List<Place> {
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

    override suspend fun fetchPhotoUri(photoMetadatas: List<PhotoMetadata>): URI? {
        return URI(photoMetadatas.first().authorAttributions?.asList()?.first()?.photoUri)
    }

}
