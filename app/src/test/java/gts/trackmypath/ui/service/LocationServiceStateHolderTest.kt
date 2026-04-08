package gts.trackmypath.ui.service

import app.cash.turbine.test
import gts.trackmypath.domain.route.RouteId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LocationServiceStateHolderTest {

    @Test
    fun `initial state is not running and has no route`() = runTest {
        val (locationServiceStateHolder, _) = createSubject()

        locationServiceStateHolder.trackingState.test {
            val initialState = awaitItem()
            assertEquals(expected = false, actual = initialState.isRunning)
            assertEquals(expected = null, actual = initialState.activeRouteId)
        }
    }

    @Test
    fun `setServiceRunning updates the flow state correctly`() = runTest {
        val (locationServiceStateHolder, _) = createSubject()

        locationServiceStateHolder.trackingState.test {
            awaitItem() // drop initial state

            val activeRouteId = RouteId(42)
            locationServiceStateHolder.setServiceRunning(isRunning = true, activeRouteId = activeRouteId)

            val updatedState = awaitItem()
            assertTrue(actual = updatedState.isRunning)
            assertEquals(expected = activeRouteId, actual = updatedState.activeRouteId)
        }
    }

    @Test
    fun `startTracking delegates to locationServiceController`() = runTest {
        val (locationServiceStateHolder, locationServiceControllerFake) = createSubject()
        val routeId = RouteId(10)

        locationServiceStateHolder.startTracking(routeId)

        assertEquals(expected = routeId, actual = locationServiceControllerFake.lastStartedRouteId)
    }

    @Test
    fun `stopTracking delegates to locationServiceController`() = runTest {
        val (locationServiceStateHolder, locationServiceControllerFake) = createSubject()

        locationServiceStateHolder.stopTracking()

        assertTrue(actual = locationServiceControllerFake.stopServiceCalled)
    }

    private fun createSubject(): Pair<LocationServiceStateHolderImpl, LocationServiceControllerFake> {
        val locationServiceControllerFake = LocationServiceControllerFake()
        val locationServiceStateHolder = LocationServiceStateHolderImpl(locationServiceController = locationServiceControllerFake)
        return Pair(locationServiceStateHolder, locationServiceControllerFake)
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
