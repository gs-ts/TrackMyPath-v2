package gts.trackmypath.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import gts.trackmypath.domain.tracking.RouteTracker
import gts.trackmypath.tracking.LocationServiceController
import gts.trackmypath.tracking.LocationServiceControllerImpl
import gts.trackmypath.tracking.ServiceRouteTracker

@Module
@InstallIn(SingletonComponent::class)
interface LocationServiceModule {

    @Binds
    fun bindRouteTracker(
        serviceRouteTracker: ServiceRouteTracker
    ): RouteTracker

    @Binds
    fun bindLocationServiceController(
        locationServiceControllerImpl: LocationServiceControllerImpl
    ): LocationServiceController
}
