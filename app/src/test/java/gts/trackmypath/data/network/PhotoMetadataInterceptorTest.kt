package gts.trackmypath.data.network

import android.content.ContextWrapper
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.domain.photometadata.PhotoMetadataRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.assertEquals

class PhotoMetadataInterceptorTest {

    @Test
    fun `intercept uses original URI if image loads successfully`() = runTest {
        val mockData = PhotoMetadata(
            id = 1L,
            placeId = "place123",
            photoUri = "https://example.com/original.jpg",
            location = PhotoMetadata.Location(latitude = 0.0, longitude = 0.0)
        )
        val initialRequest = ImageRequest.Builder(context = FakeContext()).data(mockData).build()
        val successResult = SuccessResult(
            image = mockImage(),
            request = initialRequest,
            dataSource = coil3.decode.DataSource.MEMORY
        )

        var refreshCalled = false
        val mockRepository = object : PhotoMetadataRepository {
            override suspend fun fetchPhotoMetadataForLocation(
                routeId: gts.trackmypath.domain.route.RouteId,
                location: PhotoMetadata.Location
            ): Result<Unit> = Result.success(Unit)

            override suspend fun refreshAndSavePhotoUri(placeId: String, expiredPhotoUri: String): String? {
                refreshCalled = true
                return "https://example.com/refreshed.jpg"
            }
        }

        val interceptor = PhotoMetadataInterceptor(mockRepository)

        var capturedRequest: ImageRequest = initialRequest
        val chain = object : Interceptor.Chain {
            override val request: ImageRequest get() = capturedRequest
            override val size: coil3.size.Size = coil3.size.Size.ORIGINAL
            
            override fun withRequest(request: ImageRequest): Interceptor.Chain {
                capturedRequest = request // Capture it!
                return this
            }
            override fun withSize(size: coil3.size.Size): Interceptor.Chain {
                return this
            }
            override suspend fun proceed(): coil3.request.ImageResult {
                return successResult
            }
        }

        val result = interceptor.intercept(chain)

        assertEquals(expected = successResult, actual = result)
        assertTrue(message = "Repository refresh should NOT have been called", actual = !refreshCalled)
    }

    @Test
    fun `intercept refreshes URI when load fails with 403 Forbidden`() = runTest {
        val mockData = PhotoMetadata(
            id = 1L,
            placeId = "place123",
            photoUri = "https://example.com/original.jpg",
            location = PhotoMetadata.Location(0.0, 0.0)
        )
        
        val initialRequest = ImageRequest.Builder(FakeContext()).data(mockData).build()
        val errorResult = ErrorResult(
            image = null,
            request = initialRequest, 
            throwable = RuntimeException("HTTP 403 Forbidden")
        )
        val successResult = SuccessResult(
            image = mockImage(),
            request = initialRequest,
            dataSource = coil3.decode.DataSource.NETWORK
        )

        var refreshCalled = false
        val mockRepository = object : PhotoMetadataRepository {
            override suspend fun fetchPhotoMetadataForLocation(
                routeId: gts.trackmypath.domain.route.RouteId,
                location: PhotoMetadata.Location
            ): Result<Unit> = Result.success(value = Unit)

            override suspend fun refreshAndSavePhotoUri(placeId: String, expiredPhotoUri: String): String? {
                refreshCalled = true
                return "https://example.com/refreshed.jpg"
            }
        }

        val interceptor = PhotoMetadataInterceptor(mockRepository)

        var isRetry = false
        var capturedRequest: ImageRequest = initialRequest
        val chain = object : Interceptor.Chain {
            override val request: ImageRequest get() = capturedRequest
            override val size: coil3.size.Size = coil3.size.Size.ORIGINAL
            
            override fun withRequest(request: ImageRequest): Interceptor.Chain {
                capturedRequest = request // Capture it!
                return this
            }
            override fun withSize(size: coil3.size.Size): Interceptor.Chain {
                return this
            }
            override suspend fun proceed(): coil3.request.ImageResult {
                return if (isRetry) {
                    // Verify that the interceptor actually used the new URI for the retry
                    assertEquals("https://example.com/refreshed.jpg", capturedRequest.data)
                    successResult
                } else {
                    isRetry = true
                    errorResult
                }
            }
        }

        val result = interceptor.intercept(chain)

        assertEquals(expected = successResult, actual = result)
        assertTrue(message = "Repository refresh SHOULD have been called", actual = refreshCalled)
    }

    @Test
    fun `intercept does not refresh URI when load fails with UnknownHostException`() = runTest {
        val mockData = PhotoMetadata(
            id = 1L,
            placeId = "place123",
            photoUri = "https://example.com/original.jpg",
            location = PhotoMetadata.Location(0.0, 0.0)
        )
        
        val initialRequest = ImageRequest.Builder(FakeContext()).data(mockData).build()
        val errorResult = ErrorResult(
            image = null,
            request = initialRequest, 
            throwable = java.net.UnknownHostException("Unable to resolve host")
        )

        var refreshCalled = false
        val mockRepository = object : PhotoMetadataRepository {
            override suspend fun fetchPhotoMetadataForLocation(
                routeId: gts.trackmypath.domain.route.RouteId,
                location: PhotoMetadata.Location
            ): Result<Unit> = Result.success(value = Unit)

            override suspend fun refreshAndSavePhotoUri(placeId: String, expiredPhotoUri: String): String? {
                refreshCalled = true
                return "https://example.com/refreshed.jpg"
            }
        }

        val interceptor = PhotoMetadataInterceptor(mockRepository)

        var capturedRequest: ImageRequest = initialRequest
        val chain = object : Interceptor.Chain {
            override val request: ImageRequest get() = capturedRequest
            override val size: coil3.size.Size = coil3.size.Size.ORIGINAL
            
            override fun withRequest(request: ImageRequest): Interceptor.Chain {
                capturedRequest = request
                return this
            }
            override fun withSize(size: coil3.size.Size): Interceptor.Chain {
                return this
            }
            override suspend fun proceed(): coil3.request.ImageResult {
                return errorResult
            }
        }

        val result = interceptor.intercept(chain)

        assertEquals(expected = errorResult, actual = result)
        assertTrue(message = "Repository refresh should NOT have been called for offline errors", actual = !refreshCalled)
    }

    @Test
    fun `intercept handles null from repository gracefully`() = runTest {
        val mockData = PhotoMetadata(
            id = 1L,
            placeId = "place123",
            photoUri = "https://example.com/original.jpg",
            location = PhotoMetadata.Location(0.0, 0.0)
        )
        
        val initialRequest = ImageRequest.Builder(FakeContext()).data(mockData).build()
        val errorResult = ErrorResult(
            image = null,
            request = initialRequest, 
            throwable = RuntimeException("HTTP 403 Forbidden")
        )

        var refreshCalled = false
        val mockRepository = object : PhotoMetadataRepository {
            override suspend fun fetchPhotoMetadataForLocation(
                routeId: gts.trackmypath.domain.route.RouteId,
                location: PhotoMetadata.Location
            ): Result<Unit> = Result.success(value = Unit)

            override suspend fun refreshAndSavePhotoUri(placeId: String, expiredPhotoUri: String): String? {
                refreshCalled = true
                return null // Google API failed!
            }
        }

        val interceptor = PhotoMetadataInterceptor(mockRepository)

        var isRetry = false
        var capturedRequest: ImageRequest = initialRequest
        val chain = object : Interceptor.Chain {
            override val request: ImageRequest get() = capturedRequest
            override val size: coil3.size.Size = coil3.size.Size.ORIGINAL
            
            override fun withRequest(request: ImageRequest): Interceptor.Chain {
                capturedRequest = request
                return this
            }
            override fun withSize(size: coil3.size.Size): Interceptor.Chain {
                return this
            }
            override suspend fun proceed(): coil3.request.ImageResult {
                if (isRetry) {
                    throw IllegalStateException("Should not retry if URI is null")
                }
                isRetry = true
                return errorResult
            }
        }

        val result = interceptor.intercept(chain)

        assertEquals(expected = errorResult, actual = result)
        assertTrue(message = "Repository refresh SHOULD have been called", actual = refreshCalled)
    }

    private fun mockImage(): coil3.Image = object : coil3.Image {
        override val size: Long = 0L
        override val width: Int = 100
        override val height: Int = 100
        override val shareable: Boolean = true
        override fun draw(canvas: coil3.Canvas) {}
    }
}

// A simple fake to pass into Coil's ImageRequest Builder in tests
class FakeContext : ContextWrapper(null) {
    override fun getApplicationContext(): android.content.Context = this
}
