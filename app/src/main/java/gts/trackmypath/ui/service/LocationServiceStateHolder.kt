package gts.trackmypath.ui.service

import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.ui.service.LocationService.Companion.EXTRA_ROUTE_ID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

interface LocationServiceStateHolder {

    fun setServiceRunning(
        isRunning: Boolean,
        activeRouteId: RouteId? = null
    )
}

interface LocationServiceManager {

    val trackingState: StateFlow<TrackingState>

    fun startTracking(routeId: RouteId)

    fun stopTracking()
}

@Singleton
class LocationServiceStateHolderImpl @Inject constructor(
    @param:ApplicationContext private val applicationContext: Context
) : LocationServiceStateHolder, LocationServiceManager {

    private val _trackingState = MutableStateFlow(TrackingState())
    override val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

    init {
        Log.d("LocationServiceStateHolder", "init")
    }

    override fun setServiceRunning(
        isRunning: Boolean,
        activeRouteId: RouteId?
    ) {
        Log.d("LocationServiceStateHolder", "setServiceRunning: $isRunning")
        _trackingState.update { state ->
            state.copy(
                isRunning = isRunning,
                activeRouteId = activeRouteId,
            )
        }
    }

    override fun startTracking(routeId: RouteId) {
        Log.d("LocationServiceStateHolder", "startTracking for routeId: ${routeId.id}")
        val locationServiceIntent = Intent(applicationContext, LocationService::class.java)
        locationServiceIntent.putExtra(EXTRA_ROUTE_ID, routeId.id)
        applicationContext.startForegroundService(locationServiceIntent)
    }

    override fun stopTracking() {
        Log.d("LocationServiceStateHolder", "stopTracking")
        val locationServiceIntent = Intent(applicationContext, LocationService::class.java)
        applicationContext.stopService(locationServiceIntent)
    }
}

data class TrackingState(
    val isRunning: Boolean = false,
    val activeRouteId: RouteId? = null
)
