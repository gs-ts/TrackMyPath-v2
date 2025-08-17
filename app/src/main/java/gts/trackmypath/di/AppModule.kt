package gts.trackmypath.di

import android.content.Context
import android.util.Log
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import gts.trackmypath.BuildConfig
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    private val placesApiKey = BuildConfig.PLACES_API_KEY

    @Provides
    @Singleton
    fun providePlacesClient(
        @ApplicationContext applicationContext: Context
    ): PlacesClient {
        if (placesApiKey.isEmpty() || placesApiKey == "DEFAULT_API_KEY") {
            Log.e("PlacesClient", "Places API key not configured")
            error("Places API key not configured")
        }

        Places.initializeWithNewPlacesApiEnabled(applicationContext, placesApiKey)

        return Places.createClient(applicationContext)
    }
}
