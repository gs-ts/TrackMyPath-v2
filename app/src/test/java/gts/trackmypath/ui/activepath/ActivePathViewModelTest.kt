package gts.trackmypath.ui.activepath

import app.cash.turbine.test
import gts.trackmypath.MainDispatcherRule
import gts.trackmypath.domain.route.DeleteRouteWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.FinishRouteUseCase
import gts.trackmypath.domain.route.ObserveRouteWithPhotoMetadataContract
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.RouteRepository
import gts.trackmypath.domain.route.RouteWithPhotoMetadata
import gts.trackmypath.domain.route.StartRouteUseCase
import gts.trackmypath.ui.service.LocationServiceManager
import gts.trackmypath.ui.service.TrackingState
import kotlinx.collections.immutable.persistentListOf
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
import kotlin.time.Instant

class ActivePathViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `initial state is correct`() = runTest {
        val (viewModel, _, _) = createViewModel()

        assertEquals(ActivePathViewModel.State(), viewModel.state.value)
    }

    @Test
    fun `when start button is clicked then updates state and starts tracking`() = runTest {
        val (viewModel, fakeLocationService, fakeRouteRepository) = createViewModel()

        viewModel.onStartTrackPathClick()

        val startedRouteId = fakeRouteRepository.lastStartedRouteId
        assertEquals(expected = RouteId(1), actual = startedRouteId) // our fake generates RouteId(1)
        assertEquals(expected = startedRouteId, actual = fakeLocationService.lastStartedRouteId)

        assertEquals(expected = startedRouteId, actual = viewModel.state.value.ongoingRouteId)
    }

    @Test
    fun `when stop button is clicked then stops tracking and shows NameRouteDialog`() = runTest {
        val (viewModel, fakeLocationService, _) = createViewModel()

        viewModel.onStopTrackPathClick()

        assertTrue(actual = fakeLocationService.stopTrackingCalled)
        assertTrue(actual = viewModel.state.value.showNameRouteDialog)
    }

    @Test
    fun `onRouteNameChange updates routeNameInput state`() = runTest {
        val (viewModel, _, _) = createViewModel()

        viewModel.onRouteNameChange(newRouteName = "My Cool Route")

        assertEquals(expected = "My Cool Route", actual = viewModel.state.value.routeNameInput)
    }

    @Test
    fun `when confirm in NameRouteDialog is clicked then finishes route and clears state`() = runTest {
        val (viewModel, _, fakeRouteRepository) = createViewModel()

        viewModel.onStartTrackPathClick() // Setup some initial state
        viewModel.onRouteNameChange(newRouteName = "My Cool Route")

        viewModel.onConfirmNameRouteDialogClick()

        // Verify repository interaction
        val finishedRoute = fakeRouteRepository.finishedRoutes.first()
        assertEquals(expected = RouteId(1), actual = finishedRoute.first)
        assertEquals(expected = "My Cool Route", actual = finishedRoute.second)

        // Verify state is cleared
        val state = viewModel.state.value
        assertNull(actual = state.ongoingRouteId)
        assertFalse(actual = state.showNameRouteDialog)
        assertEquals(expected = "", actual = state.routeNameInput)
        assertEquals(expected = persistentListOf<Any>(), actual = state.photos)
        assertTrue(actual = state.showSnackbarRouteSavedConfirmation)
    }

    @Test
    fun `when onHideSnackbarRouteSavedConfirmation is called then resets snackbar state`() = runTest {
        val (viewModel, _, _) = createViewModel()

        viewModel.onStartTrackPathClick()
        // trigger the action that makes the showSnackbarRouteSavedConfirmation true
        viewModel.onConfirmNameRouteDialogClick()

        // check to ensure it is actually true before we hide it
        assertTrue(actual = viewModel.state.value.showSnackbarRouteSavedConfirmation)

        // call the hide function
        viewModel.onHideSnackbarRouteSavedConfirmation()

        assertFalse(actual = viewModel.state.value.showSnackbarRouteSavedConfirmation)
    }

    @Test
    fun `when dismiss in NameRouteDialog is clicked then deletes pending route and clears state`() = runTest {
        val (viewModel, _, fakeRouteRepository) = createViewModel()

        viewModel.onStartTrackPathClick() // Setup initial state

        viewModel.onDismissNameRouteDialogClick()

        // Verify repository interaction
        val deletedRouteId = fakeRouteRepository.deletedRouteIds.first()
        assertEquals(expected = RouteId(1), actual = deletedRouteId)

        // Verify state is cleared
        val state = viewModel.state.value
        assertNull(actual = state.ongoingRouteId)
        assertFalse(actual = state.showNameRouteDialog)
        assertEquals(expected = "", actual = state.routeNameInput)
        assertEquals(expected = persistentListOf<Any>(), actual = state.photos)
    }

    @Test
    fun `tracking state updates from location service are reflected in viewmodel state`() = runTest {
        val (viewModel, fakeLocationService, _) = createViewModel()

        viewModel.state.test {
            // initial state
            val initial = awaitItem()
            assertFalse(actual = initial.isLocationServiceRunning)
            assertNull(actual = initial.ongoingRouteId)

            // emit running state
            val activeRouteId = RouteId(99)
            fakeLocationService.emitTrackingState(TrackingState(isRunning = true, activeRouteId = activeRouteId))

            val runningState = awaitItem()
            assertTrue(actual = runningState.isLocationServiceRunning)
            assertEquals(expected = activeRouteId, actual = runningState.ongoingRouteId)

            // emit stopped state
            fakeLocationService.emitTrackingState(TrackingState(isRunning = false, activeRouteId = null))

            val stoppedState = awaitItem()
            assertFalse(actual = stoppedState.isLocationServiceRunning)
        }
    }

    // --- Helper classes and setup ---

    private fun createViewModel(): Triple<ActivePathViewModel, LocationServiceManagerFake, RouteRepositoryFake> {
        val locationServiceManagerFake = LocationServiceManagerFake()
        val routeRepositoryFake = RouteRepositoryFake()

        val activePathViewModel = ActivePathViewModel(
            locationServiceManager = locationServiceManagerFake,
            startRouteUseCase = StartRouteUseCase(routeRepositoryFake),
            finishRouteUseCase = FinishRouteUseCase(routeRepositoryFake),
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

        var lastStartedRouteId: RouteId? = null
        var stopTrackingCalled = false

        override fun startTracking(routeId: RouteId) {
            lastStartedRouteId = routeId
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

    class RouteRepositoryFake : RouteRepository {
        var lastStartedRouteId: RouteId? = null
        val finishedRoutes = mutableListOf<Pair<RouteId, String>>()
        val deletedRouteIds = mutableListOf<RouteId>()

        override suspend fun startRoute(): RouteId {
            val newRouteId = RouteId(1)
            lastStartedRouteId = newRouteId
            return newRouteId
        }

        override suspend fun finishRoute(routeId: RouteId, displayName: String, metadata: Map<String, String>) {
            finishedRoutes.add(Pair(routeId, displayName))
        }

        override suspend fun deleteRoute(routeId: RouteId) {
            deletedRouteIds.add(routeId)
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
