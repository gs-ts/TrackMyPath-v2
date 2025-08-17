package gts.trackmypath.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import gts.trackmypath.data.PhotoRepositoryImpl
import gts.trackmypath.domain.PhotoRepository

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    fun bindPhotoRepository(
        photoRepositoryImpl: PhotoRepositoryImpl
    ): PhotoRepository
}
