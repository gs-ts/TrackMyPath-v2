package gts.trackmypath.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import gts.trackmypath.data.network.GooglePlacesClient
import gts.trackmypath.data.network.GooglePlacesClientImpl
import gts.trackmypath.data.repository.PhotoMetadataRepositoryImpl
import gts.trackmypath.data.repository.RouteRepositoryImpl
import gts.trackmypath.domain.photometadata.PhotoMetadataRepository
import gts.trackmypath.domain.route.RouteRepository

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindGooglePlacesClient(
        googlePlacesClientImpl: GooglePlacesClientImpl
    ): GooglePlacesClient

    @Binds
    fun bindPhotoMetadataRepository(
        photoMetadataRepositoryImpl: PhotoMetadataRepositoryImpl
    ): PhotoMetadataRepository

    @Binds
    fun bindRouteRepository(
        routeRepositoryImpl: RouteRepositoryImpl
    ): RouteRepository
}
