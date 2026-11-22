package gts.trackmypath.tracking

import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.tracking.TrackingState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceRouteTrackerTest {

    @Test
    fun `initial state is idle`() {
        val (routeTracker, _) = createSubject()

        assertEquals(expected = TrackingState.Idle, actual = routeTracker.state.value)
    }

    @Test
    fun `onTrackingStarted reports the tracked route`() {
        val (routeTracker, _) = createSubject()

        routeTracker.onTrackingStarted(routeId = RouteId(42))

        assertEquals(expected = TrackingState.Tracking(RouteId(42)), actual = routeTracker.state.value)
    }

    @Test
    fun `onTrackingStopped reports idle`() {
        val (routeTracker, _) = createSubject()
        routeTracker.onTrackingStarted(routeId = RouteId(42))

        routeTracker.onTrackingStopped()

        assertEquals(expected = TrackingState.Idle, actual = routeTracker.state.value)
    }

    @Test
    fun `start starts the service for the route, and the state waits for the service`() {
        val (routeTracker, locationServiceControllerFake) = createSubject()

        routeTracker.start(routeId = RouteId(10))

        assertEquals(expected = RouteId(10), actual = locationServiceControllerFake.lastStartedRouteId)
        // Android can refuse to start the service, so only the service reports that tracking started.
        assertEquals(expected = TrackingState.Idle, actual = routeTracker.state.value)
    }

    @Test
    fun `stop stops the service`() {
        val (routeTracker, locationServiceControllerFake) = createSubject()

        routeTracker.stop()

        assertTrue(actual = locationServiceControllerFake.stopServiceCalled)
    }

    private fun createSubject(): Pair<ServiceRouteTracker, LocationServiceControllerFake> {
        val locationServiceControllerFake = LocationServiceControllerFake()
        val routeTracker = ServiceRouteTracker(locationServiceController = locationServiceControllerFake)
        return Pair(routeTracker, locationServiceControllerFake)
    }

    class LocationServiceControllerFake : LocationServiceController {
        var lastStartedRouteId: RouteId? = null
        var stopServiceCalled = false

        override fun startService(routeId: RouteId) {
            lastStartedRouteId = routeId
        }

        override fun stopService() {
            stopServiceCalled = true
        }
    }
}
