package gts.trackmypath.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import gts.trackmypath.data.PhotoRepositoryImpl
import gts.trackmypath.data.RouteRepositoryImpl
import gts.trackmypath.data.network.GooglePlacesClient
import gts.trackmypath.data.network.GooglePlacesClientImpl
import gts.trackmypath.domain.photo.PhotoRepository
import gts.trackmypath.domain.route.RouteRepository

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindGooglePlacesClientI(
        googlePlacesClientImpl: GooglePlacesClientImpl
    ): GooglePlacesClient

    @Binds
    fun bindPhotoRepository(
        photoRepositoryImpl: PhotoRepositoryImpl
    ): PhotoRepository

    @Binds
    fun bindRouteRepository(
        routeRepositoryImpl: RouteRepositoryImpl
    ): RouteRepository
}
