package gts.trackmypath.data.network

import android.util.Log
import coil3.intercept.Interceptor
import coil3.network.HttpException
import coil3.request.ErrorResult
import coil3.request.ImageResult
import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.domain.photometadata.PhotoMetadataRepository
import javax.inject.Inject

class PhotoMetadataInterceptor @Inject constructor(
    private val repository: PhotoMetadataRepository
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val photoMetadata = request.data // photoMetadata comes from DB

        if (photoMetadata is PhotoMetadata) {
            // 1. tell Coil to load the URL from the DB, but cache it using the placeId
            var newRequest = request.newBuilder()
                .data(data = photoMetadata.photoUri)
                .memoryCacheKey(key = photoMetadata.placeId) // stable cache key regardless of URL expiration
                .diskCacheKey(key = photoMetadata.placeId) // stable cache key regardless of URL expiration
                .build()

            Log.d("PhotoMetadataInterceptor", "fetch photo for placeId = ${photoMetadata.placeId}")
            // 2. optimistically try to load the image
            var result = chain.withRequest(newRequest).proceed()

            // 3. if the URL was expired, Coil returns an ErrorResult
            if (result is ErrorResult && isUrlExpired(result.throwable)) {
                Log.w("PhotoMetadataInterceptor", "URL expired for placeId = ${photoMetadata.placeId}")

                // 4. fetch a fresh URI from Google Places API, passing the known expired URI
                val freshPhotoUri = repository.refreshAndSavePhotoUri(
                    placeId = photoMetadata.placeId,
                    expiredPhotoUri = photoMetadata.photoUri
                )

                if (freshPhotoUri != null) {
                    // 5. retry the request with the fresh URI
                    newRequest = newRequest.newBuilder()
                        .data(freshPhotoUri)
                        .build()

                    result = chain.withRequest(newRequest).proceed()
                }
            }

            return result
        }

        return chain.proceed()
    }

    // An expired signed URL answers 403 Forbidden or 404 Not Found. Coil reports any other non-2xx answer
    // as an HttpException too, and its message is only "HTTP <code>", so check the code, not the message.
    private fun isUrlExpired(throwable: Throwable): Boolean =
        throwable is HttpException && throwable.response.code in EXPIRED_URL_CODES

    private companion object {
        val EXPIRED_URL_CODES = setOf(403, 404)
    }
}
