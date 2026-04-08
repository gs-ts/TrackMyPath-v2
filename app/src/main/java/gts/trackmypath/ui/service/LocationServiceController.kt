package gts.trackmypath.ui.service

import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import gts.trackmypath.domain.route.RouteId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Responsible strictly for interacting with the Android framework
 * to start and stop the physical LocationService.
 *
 */
interface LocationServiceController {

    fun startService(routeId: RouteId)

    fun stopService()
}

@Singleton
class LocationServiceControllerImpl @Inject constructor(
    @param:ApplicationContext private val applicationContext: Context
) : LocationServiceController {

    override fun startService(routeId: RouteId) {
        Log.d("LocationServiceController", "starting service for routeId: ${routeId.id}")
        val locationServiceIntent = Intent(applicationContext, LocationService::class.java).apply {
            putExtra(LocationService.EXTRA_ROUTE_ID, routeId.id)
        }
        applicationContext.startForegroundService(locationServiceIntent)
    }

    override fun stopService() {
        Log.d("LocationServiceController", "stopping service")
        val locationServiceIntent = Intent(applicationContext, LocationService::class.java)
        applicationContext.stopService(locationServiceIntent)
    }
}
