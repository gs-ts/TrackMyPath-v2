package gts.trackmypath.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import gts.trackmypath.ui.service.LocationServiceManager
import gts.trackmypath.ui.service.LocationServiceStateHolder
import gts.trackmypath.ui.service.LocationServiceStateHolderImpl

@Module
@InstallIn(SingletonComponent::class)
interface LocationServiceModule {

    @Binds
    fun bindLocationServiceStateHolder(
        locationServiceStateHolderImpl: LocationServiceStateHolderImpl
    ): LocationServiceStateHolder

    @Binds
    fun bindLocationServiceManager(
        locationServiceStateHolderImpl: LocationServiceStateHolderImpl
    ): LocationServiceManager
}
