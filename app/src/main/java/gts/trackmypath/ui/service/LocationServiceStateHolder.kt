package gts.trackmypath.ui.service

import android.util.Log
import gts.trackmypath.domain.route.RouteId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Responsible for holding the state of the LocationService.
 *
 */
interface LocationServiceStateHolder {

    fun setServiceRunning(
        isRunning: Boolean,
        activeRouteId: RouteId? = null
    )
}

/**
 * Responsible for starting and stopping the LocationService.
 *
 */
interface LocationServiceManager {

    val trackingState: StateFlow<TrackingState>

    fun startTracking(routeId: RouteId)

    fun stopTracking()
}

@Singleton
class LocationServiceStateHolderImpl @Inject constructor(
    private val locationServiceController: LocationServiceController
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
        locationServiceController.startService(routeId)
    }

    override fun stopTracking() {
        locationServiceController.stopService()
    }
}

data class TrackingState(
    val isRunning: Boolean = false,
    val activeRouteId: RouteId? = null
)
