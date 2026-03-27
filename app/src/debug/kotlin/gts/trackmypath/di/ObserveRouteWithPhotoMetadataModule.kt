package gts.trackmypath.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import gts.trackmypath.domain.route.ObserveRouteWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.ObserveRouteWithPhotoMetadataContract

@Module
@InstallIn(ViewModelComponent::class)
abstract class ObserveRouteWithPhotoMetadataModule {

    @Binds
    abstract fun bindObserveRouteWithPhotoMetadataContract(
        impl: ObserveRouteWithPhotoMetadataUseCase
    ): ObserveRouteWithPhotoMetadataContract
}
