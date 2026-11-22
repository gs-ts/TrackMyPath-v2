package gts.trackmypath.ui.activepath

import app.cash.turbine.test
import gts.trackmypath.MainDispatcherRule
import gts.trackmypath.domain.route.DeleteRouteWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.FinishRouteUseCase
import gts.trackmypath.domain.route.ObserveRouteWithPhotoMetadataContract
import gts.trackmypath.domain.route.RenameRouteUseCase
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.RouteRepository
import gts.trackmypath.domain.route.RouteWithPhotoMetadata
import gts.trackmypath.domain.route.StartRouteUseCase
import gts.trackmypath.ui.service.LocationServiceManager
import gts.trackmypath.ui.service.TrackingState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class ActivePathViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val now = Instant.parse("2026-11-18T10:00:00Z")
    private val fixedClock = object : Clock {
        override fun now(): Instant = now
    }

    @Test
    fun `initial state is correct`() = runTest {
        val (viewModel, _, _) = createViewModel()

        assertEquals(ActivePathViewModel.State(), viewModel.state.value)
    }

    @Test
    fun `when start button is clicked then updates state and starts tracking`() = runTest {
        val (viewModel, fakeLocationService, fakeRouteRepository) = createViewModel()

        viewModel.onStartTrackPathClick()

        val startedRouteId = fakeRouteRepository.routes.keys.single()
        assertEquals(expected = startedRouteId, actual = fakeLocationService.lastStartedRouteId)
        assertEquals(expected = startedRouteId, actual = viewModel.state.value.ongoingRouteId)
    }

    @Test
    fun `a second tap on start while the route is being created does nothing`() = runTest {
        val routeCreation = CompletableDeferred<Unit>()
        val (viewModel, fakeLocationService, fakeRouteRepository) = createViewModel(
            routeRepositoryFake = RouteRepositoryFake(beforeStartRoute = { routeCreation.await() })
        )

        viewModel.onStartTrackPathClick()
        viewModel.onStartTrackPathClick() // the first tap is still creating the route
        routeCreation.complete(Unit)

        assertEquals(expected = setOf(RouteId(1)), actual = fakeRouteRepository.routes.keys)
        assertEquals(expected = listOf(RouteId(1)), actual = fakeLocationService.startedRouteIds)
    }

    @Test
    fun `a tap on start while a route is tracked does nothing`() = runTest {
        val (viewModel, fakeLocationService, fakeRouteRepository) = createViewModel()
        viewModel.onStartTrackPathClick()

        viewModel.onStartTrackPathClick()

        assertEquals(expected = setOf(RouteId(1)), actual = fakeRouteRepository.routes.keys)
        assertEquals(expected = listOf(RouteId(1)), actual = fakeLocationService.startedRouteIds)
    }

    @Test
    fun `start works again after the previous route is done`() = runTest {
        val (viewModel, fakeLocationService, _) = createViewModel()
        viewModel.onStartTrackPathClick()
        viewModel.onStopTrackPathClick()
        viewModel.onAction(ActivePathViewModel.Action.OnDiscardRouteClick)

        viewModel.onStartTrackPathClick()

        assertEquals(expected = listOf(RouteId(1), RouteId(2)), actual = fakeLocationService.startedRouteIds)
    }

    @Test
    fun `when stop button is clicked then finishes the route and shows NameRouteDialog`() = runTest {
        val (viewModel, fakeLocationService, fakeRouteRepository) = createViewModel()
        viewModel.onStartTrackPathClick()

        viewModel.onStopTrackPathClick()

        assertTrue(actual = fakeLocationService.stopTrackingCalled)
        // Finished before the user names it, so the route is kept whatever they do with the dialog.
        assertEquals(expected = now, actual = fakeRouteRepository.routes.getValue(RouteId(1)).finishedAt)
        assertTrue(actual = viewModel.state.value.shouldShowNameRouteDialog)
    }

    @Test
    fun `onRouteNameChange updates routeNameInput state`() = runTest {
        val (viewModel, _, _) = createViewModel()

        viewModel.onRouteNameChange(newRouteName = "My Cool Route")

        assertEquals(expected = "My Cool Route", actual = viewModel.state.value.routeNameInput)
    }

    @Test
    fun `when confirm in NameRouteDialog is clicked then names the route and clears state`() = runTest {
        val (viewModel, _, fakeRouteRepository) = createViewModel()
        viewModel.onStartTrackPathClick()
        viewModel.onStopTrackPathClick()
        viewModel.onRouteNameChange(newRouteName = "  My Cool Route ")

        viewModel.onConfirmNameRouteDialogClick()

        val route = fakeRouteRepository.routes.getValue(RouteId(1))
        assertEquals(expected = "My Cool Route", actual = route.name)
        assertEquals(expected = now, actual = route.finishedAt)

        val state = viewModel.state.value
        assertNull(actual = state.ongoingRouteId)
        assertFalse(actual = state.shouldShowNameRouteDialog)
        assertEquals(expected = "", actual = state.routeNameInput)
        assertEquals(expected = persistentListOf<Any>(), actual = state.photos)
        assertTrue(actual = state.shouldShowSnackbarRouteSavedConfirmation)
    }

    @Test
    fun `when confirm is clicked with a blank name then keeps NameRouteDialog open`() = runTest {
        val (viewModel, _, fakeRouteRepository) = createViewModel()
        viewModel.onStartTrackPathClick()
        viewModel.onStopTrackPathClick()
        viewModel.onRouteNameChange(newRouteName = "   ")

        viewModel.onConfirmNameRouteDialogClick()

        assertNull(actual = fakeRouteRepository.routes.getValue(RouteId(1)).name)
        assertTrue(actual = viewModel.state.value.shouldShowNameRouteDialog)
    }

    @Test
    fun `when onHideSnackbarRouteSavedConfirmation is called then resets snackbar state`() = runTest {
        val (viewModel, _, _) = createViewModel()

        viewModel.onStartTrackPathClick()
        viewModel.onStopTrackPathClick()
        viewModel.onRouteNameChange(newRouteName = "My Cool Route")
        // trigger the action that makes the showSnackbarRouteSavedConfirmation true
        viewModel.onConfirmNameRouteDialogClick()

        // check to ensure it is actually true before we hide it
        assertTrue(actual = viewModel.state.value.shouldShowSnackbarRouteSavedConfirmation)

        // call the hide function
        viewModel.hideSnackbarRouteSavedConfirmation()

        assertFalse(actual = viewModel.state.value.shouldShowSnackbarRouteSavedConfirmation)
    }

    @Test
    fun `when discard in NameRouteDialog is clicked then deletes the route and clears state`() = runTest {
        val (viewModel, _, fakeRouteRepository) = createViewModel()
        viewModel.onStartTrackPathClick()
        viewModel.onStopTrackPathClick()

        viewModel.onAction(ActivePathViewModel.Action.OnDiscardRouteClick)

        assertTrue(actual = fakeRouteRepository.routes.isEmpty())

        val state = viewModel.state.value
        assertNull(actual = state.ongoingRouteId)
        assertFalse(actual = state.shouldShowNameRouteDialog)
        assertEquals(expected = persistentListOf<Any>(), actual = state.photos)
        assertFalse(actual = state.shouldShowSnackbarRouteSavedConfirmation)
    }

    @Test
    fun `tracking state updates from location service are reflected in viewmodel state`() = runTest {
        val (viewModel, fakeLocationService, _) = createViewModel()

        viewModel.state.test {
            // initial state
            val initial = awaitItem()
            assertFalse(actual = initial.isTracking)
            assertNull(actual = initial.ongoingRouteId)

            // emit running state
            val activeRouteId = RouteId(99)
            fakeLocationService.emitTrackingState(TrackingState(isRunning = true, activeRouteId = activeRouteId))

            val runningState = awaitItem()
            assertTrue(actual = runningState.isTracking)
            assertEquals(expected = activeRouteId, actual = runningState.ongoingRouteId)

            // emit stopped state
            fakeLocationService.emitTrackingState(TrackingState(isRunning = false, activeRouteId = null))

            val stoppedState = awaitItem()
            assertFalse(actual = stoppedState.isTracking)
        }
    }

    @Test
    fun `onPlaceFilterClick shows bottom sheet`() = runTest {
        val (viewModel, _, _) = createViewModel()

        viewModel.onPlaceFilterClick()

        assertTrue(actual = viewModel.state.value.shouldShowPlaceFilterBottomSheet)
    }

    @Test
    fun `given place filter bottom sheet is open when onDismissPlaceFilterBottomSheet then hides bottom sheet`() = runTest {
        val (viewModel, _, _) = createViewModel()

        viewModel.onPlaceFilterClick() // Show it first
        assertTrue(actual = viewModel.state.value.shouldShowPlaceFilterBottomSheet)

        viewModel.onClosePlaceFilterBottomSheet()
        assertFalse(actual = viewModel.state.value.shouldShowPlaceFilterBottomSheet)
    }

    // --- Helper classes and setup ---

    private fun createViewModel(
        routeRepositoryFake: RouteRepositoryFake = RouteRepositoryFake()
    ): Triple<ActivePathViewModel, LocationServiceManagerFake, RouteRepositoryFake> {
        val locationServiceManagerFake = LocationServiceManagerFake()

        val activePathViewModel = ActivePathViewModel(
            locationServiceManager = locationServiceManagerFake,
            startRouteUseCase = StartRouteUseCase(routeRepositoryFake),
            finishRouteUseCase = FinishRouteUseCase(routeRepositoryFake, fixedClock),
            renameRouteUseCase = RenameRouteUseCase(routeRepositoryFake),
            deleteRouteWithPhotoMetadataUseCase = DeleteRouteWithPhotoMetadataUseCase(routeRepositoryFake),
            observeRouteWithPhotoMetadataUseCase = object : ObserveRouteWithPhotoMetadataContract {
                override fun invoke(routeId: RouteId): Flow<RouteWithPhotoMetadata> {
                    return routeRepositoryFake.observeRouteWithPhotoMetadataById(routeId)
                }
            }
        )

        return Triple(activePathViewModel, locationServiceManagerFake, routeRepositoryFake)
    }

    class LocationServiceManagerFake : LocationServiceManager {
        private val _trackingState = MutableStateFlow(TrackingState())
        override val trackingState: StateFlow<TrackingState> = _trackingState.asStateFlow()

        val startedRouteIds = mutableListOf<RouteId>()
        val lastStartedRouteId: RouteId? get() = startedRouteIds.lastOrNull()
        var stopTrackingCalled = false

        override fun startTracking(routeId: RouteId) {
            startedRouteIds += routeId
            _trackingState.update { it.copy(isRunning = true, activeRouteId = routeId) }
        }

        override fun stopTracking() {
            stopTrackingCalled = true
            _trackingState.update { it.copy(isRunning = false) }
        }

        fun emitTrackingState(state: TrackingState) {
            _trackingState.value = state
        }
    }

    // Keeps routes in memory, so tests can check what was saved.
    class RouteRepositoryFake(
        // Runs before a route is created, so a test can keep the creation in progress.
        private val beforeStartRoute: suspend () -> Unit = {}
    ) : RouteRepository {

        data class StoredRoute(val name: String? = null, val finishedAt: Instant? = null)

        val routes = mutableMapOf<RouteId, StoredRoute>()
        private var lastRouteId = 0L

        override suspend fun startRoute(): RouteId {
            beforeStartRoute()
            val routeId = RouteId(++lastRouteId)
            routes[routeId] = StoredRoute()
            return routeId
        }

        override suspend fun finishRoute(routeId: RouteId, finishedAt: Instant, metadata: Map<String, String>) {
            routes[routeId]?.let { route -> routes[routeId] = route.copy(finishedAt = finishedAt) }
        }

        override suspend fun renameRoute(routeId: RouteId, name: String) {
            routes[routeId]?.let { route -> routes[routeId] = route.copy(name = name) }
        }

        override suspend fun deleteRoute(routeId: RouteId) {
            routes.remove(routeId)
        }

        override fun observeRouteWithPhotoMetadataById(routeId: RouteId): Flow<RouteWithPhotoMetadata> {
            return flowOf(
                RouteWithPhotoMetadata(
                    routeId = routeId,
                    displayName = "Test",
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    metadata = emptyMap(),
                    photoMetadata = emptyList()
                )
            )
        }

        override fun observeRoutesWithPhotoMetadata(): Flow<List<RouteWithPhotoMetadata>> {
            return flowOf(emptyList())
        }
    }
}
