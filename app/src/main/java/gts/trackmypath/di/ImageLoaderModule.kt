package gts.trackmypath.di

import android.content.Context
import coil3.ImageLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import gts.trackmypath.data.network.PhotoMetadataInterceptor
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        interceptor: PhotoMetadataInterceptor
    ): ImageLoader {
        return ImageLoader.Builder(context)
            .components { add(interceptor) }
            .build()
    }
}
