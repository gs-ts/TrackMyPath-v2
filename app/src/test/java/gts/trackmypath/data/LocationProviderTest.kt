package gts.trackmypath.data

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * [LocationProvider] wraps FusedLocationProviderClient, which can't run on the JVM, so these tests cover
 * [shareAsResultsIn]: the operator it uses to share location updates in the application scope.
 */
class LocationProviderTest {

    // Stands in for applicationScope: a SupervisorJob and an exception handler. In production there is no
    // handler, so anything recorded here would crash the app instead.
    private val uncaughtExceptions = mutableListOf<Throwable>()
    private lateinit var applicationScope: CoroutineScope

    private fun TestScope.createApplicationScope(): CoroutineScope {
        applicationScope = CoroutineScope(
            SupervisorJob() +
                StandardTestDispatcher(testScheduler) +
                CoroutineExceptionHandler { _, exception -> uncaughtExceptions += exception }
        )
        return applicationScope
    }

    @AfterTest
    fun tearDown() {
        applicationScope.cancel()
    }

    @Test
    fun `upstream failure is delivered to the collector instead of crashing the application scope`() = runTest {
        // e.g. requestLocationUpdates fails because Google Play services is missing or outdated
        val failure = IllegalStateException("Location updates unavailable")
        val locationUpdates = flow {
            emit(1)
            throw failure
        }.shareAsResultsIn(scope = createApplicationScope())

        val received = mutableListOf<Result<Int>>()
        backgroundScope.launch { locationUpdates.toList(received) }
        runCurrent()

        assertEquals(expected = listOf(Result.success(1), Result.failure(failure)), actual = received)
        assertTrue(actual = uncaughtExceptions.isEmpty(), message = "Uncaught (would crash): $uncaughtExceptions")
    }

    @Test
    fun `a new collector after a failure restarts the upstream so the user can try again`() = runTest {
        var upstreamStarts = 0
        val locationUpdates = flow<Int> {
            upstreamStarts++
            throw IllegalStateException("Location updates unavailable")
        }.shareAsResultsIn(scope = createApplicationScope())

        // 1. Tracking starts and fails, so the service stops collecting
        assertTrue(actual = locationUpdates.first().isFailure)
        runCurrent() // time passes before the user taps Play again

        // 2. The user tries again: location updates are requested again, not served from a stale failure
        assertTrue(actual = locationUpdates.first().isFailure)
        assertEquals(expected = 2, actual = upstreamStarts)
        assertTrue(actual = uncaughtExceptions.isEmpty(), message = "Uncaught (would crash): $uncaughtExceptions")
    }
}
