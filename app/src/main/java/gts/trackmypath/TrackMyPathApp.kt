package gts.trackmypath

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import jakarta.inject.Inject

@HiltAndroidApp
class TrackMyPathApp : Application(), SingletonImageLoader.Factory {

    @Inject lateinit var imageLoader: ImageLoader

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return imageLoader
    }
}
