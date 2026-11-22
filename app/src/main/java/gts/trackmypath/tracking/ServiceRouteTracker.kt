package gts.trackmypath.tracking

import android.util.Log
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.tracking.RouteTracker
import gts.trackmypath.domain.tracking.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks routes with [LocationService]: [start] and [stop] send commands to the service,
 * and [state] is what the service reports back through [onTrackingStarted] and [onTrackingStopped].
 */
@Singleton
class ServiceRouteTracker @Inject constructor(
    private val locationServiceController: LocationServiceController
) : RouteTracker {

    private val _state = MutableStateFlow<TrackingState>(TrackingState.Idle)
    override val state: StateFlow<TrackingState> = _state.asStateFlow()

    override fun start(routeId: RouteId) {
        locationServiceController.startService(routeId)
    }

    override fun stop() {
        locationServiceController.stopService()
    }

    /** Called by [LocationService] when it runs in the foreground and tracks [routeId]. */
    fun onTrackingStarted(routeId: RouteId) {
        Log.d("ServiceRouteTracker", "onTrackingStarted: ${routeId.id}")
        _state.value = TrackingState.Tracking(routeId = routeId)
    }

    /** Called by [LocationService] when it stops, or when Android doesn't let it start, so nothing is tracked. */
    fun onTrackingStopped() {
        Log.d("ServiceRouteTracker", "onTrackingStopped")
        _state.value = TrackingState.Idle
    }
}
