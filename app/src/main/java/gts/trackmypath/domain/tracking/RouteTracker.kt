package gts.trackmypath.domain.tracking

import gts.trackmypath.domain.route.RouteId
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the tracking session: whether a route is being tracked, and which one.
 *
 * [state] changes when tracking actually starts or stops, not when [start] or [stop] is called,
 * because Android can refuse to start tracking.
 */
interface RouteTracker {

    val state: StateFlow<TrackingState>

    fun start(routeId: RouteId)

    fun stop()
}

sealed interface TrackingState {

    data object Idle : TrackingState

    data class Tracking(val routeId: RouteId) : TrackingState
}
