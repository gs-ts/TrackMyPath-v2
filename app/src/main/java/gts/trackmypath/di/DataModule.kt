package gts.trackmypath.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import gts.trackmypath.data.GooglePlacesClient
import gts.trackmypath.data.GooglePlacesClientImpl
import gts.trackmypath.data.PhotoRepositoryImpl
import gts.trackmypath.domain.PhotoRepository

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
}
