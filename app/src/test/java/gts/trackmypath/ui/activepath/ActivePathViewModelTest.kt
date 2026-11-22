package gts.trackmypath.ui.activepath

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import gts.trackmypath.MainDispatcherRule
import gts.trackmypath.domain.Coordinates
import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.domain.route.DeleteRouteWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.ObserveRouteWithPhotoMetadataContract
import gts.trackmypath.domain.route.RenameRouteUseCase
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.RouteRepository
import gts.trackmypath.domain.route.RouteWithPhotoMetadata
import gts.trackmypath.domain.tracking.RouteTracker
import gts.trackmypath.domain.tracking.StartTrackingUseCase
import gts.trackmypath.domain.tracking.StopTrackingUseCase
import gts.trackmypath.domain.tracking.TrackingState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
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

    private val photos = listOf(photo(id = 1), photo(id = 2))

    @Test
    fun `initial state is correct`() = runTest {
        val (viewModel, _, _) = createViewModel()

        assertEquals(ActivePathViewModel.State(), viewModel.state.value)
    }

    @Test
    fun `when the screen opens while a route is tracked then shows that route`() = runTest {
        // e.g. the user reopens the app from the tracking notification
        val (viewModel, _, _) = createViewModel(
            routeTrackerFake = RouteTrackerFake(initialState = TrackingState.Tracking(RouteId(5))),
            routeRepositoryFake = RouteRepositoryFake().apply { emitPhotos(RouteId(5), photos) }
        )

        assertTrue(actual = viewModel.state.value.isTracking)
        assertEquals(expected = photos, actual = viewModel.state.value.photos)
    }

    @Test
    fun `when start button is clicked then tracks a new route`() = runTest {
        val (viewModel, fakeRouteTracker, fakeRouteRepository) = createViewModel()

        viewModel.onStartTrackPathClick()

        val startedRouteId = fakeRouteRepository.routes.keys.single()
        assertEquals(expected = TrackingState.Tracking(startedRouteId), actual = fakeRouteTracker.state.value)
        assertTrue(actual = viewModel.state.value.isTracking)
        assertTrue(actual = viewModel.state.value.isStreamLoading) // no photos yet
    }

    @Test
    fun `a second tap on start while the route is being created does nothing`() = runTest {
        val routeCreation = CompletableDeferred<Unit>()
        val (viewModel, fakeRouteTracker, fakeRouteRepository) = createViewModel(
            routeRepositoryFake = RouteRepositoryFake(beforeStartRoute = { routeCreation.await() })
        )

        viewModel.onStartTrackPathClick()
        viewModel.onStartTrackPathClick() // the first tap is still creating the route
        routeCreation.complete(Unit)

        assertEquals(expected = setOf(RouteId(1)), actual = fakeRouteRepository.routes.keys)
        assertEquals(expected = listOf(RouteId(1)), actual = fakeRouteTracker.startedRouteIds)
    }

    @Test
    fun `a tap on start while a route is tracked does nothing`() = runTest {
        val (viewModel, fakeRouteTracker, fakeRouteRepository) = createViewModel()
        viewModel.onStartTrackPathClick()

        viewModel.onStartTrackPathClick()

        assertEquals(expected = setOf(RouteId(1)), actual = fakeRouteRepository.routes.keys)
        assertEquals(expected = listOf(RouteId(1)), actual = fakeRouteTracker.startedRouteIds)
    }

    @Test
    fun `start works again after the previous route is done`() = runTest {
        val (viewModel, fakeRouteTracker, _) = createViewModel()
        viewModel.onStartTrackPathClick()
        viewModel.onStopTrackPathClick()
        viewModel.onAction(ActivePathViewModel.Action.OnDiscardRouteClick)

        viewModel.onStartTrackPathClick()

        assertEquals(expected = listOf(RouteId(1), RouteId(2)), actual = fakeRouteTracker.startedRouteIds)
    }

    @Test
    fun `when stop button is clicked then stops tracking, finishes the route and shows NameRouteDialog`() = runTest {
        val (viewModel, fakeRouteTracker, fakeRouteRepository) = createViewModel()
        viewModel.onStartTrackPathClick()

        viewModel.onStopTrackPathClick()

        assertEquals(expected = TrackingState.Idle, actual = fakeRouteTracker.state.value)
        // Finished before the user names it, so the route is kept whatever they do with the dialog.
        assertEquals(expected = now, actual = fakeRouteRepository.routes.getValue(RouteId(1)).finishedAt)
        assertTrue(actual = viewModel.state.value.popups.shouldShowNameRouteDialog)
        assertFalse(actual = viewModel.state.value.isTracking)
    }

    @Test
    fun `a second tap on stop before tracking has stopped does nothing`() = runTest {
        // The service stops a moment after Stop, so a quick second tap still finds a Stop button.
        val (viewModel, fakeRouteTracker, _) = createViewModel(
            routeTrackerFake = RouteTrackerFake(reportsStopRightAway = false)
        )
        viewModel.onStartTrackPathClick()
        viewModel.onStopTrackPathClick()

        viewModel.onStopTrackPathClick()

        assertEquals(expected = 1, actual = fakeRouteTracker.stopRequests)
        assertTrue(actual = viewModel.state.value.popups.shouldShowNameRouteDialog)
    }

    @Test
    fun `shows the photos of the tracked route as they are found`() = runTest {
        val (viewModel, _, fakeRouteRepository) = createViewModel()
        viewModel.onStartTrackPathClick()

        fakeRouteRepository.emitPhotos(RouteId(1), photos)

        assertEquals(expected = photos, actual = viewModel.state.value.photos)
        assertFalse(actual = viewModel.state.value.isStreamLoading)
    }

    @Test
    fun `after stop the photos stay on screen while NameRouteDialog is open`() = runTest {
        val (viewModel, _, fakeRouteRepository) = createViewModel()
        viewModel.onStartTrackPathClick()
        fakeRouteRepository.emitPhotos(RouteId(1), photos)

        viewModel.onStopTrackPathClick()

        assertTrue(actual = viewModel.state.value.popups.shouldShowNameRouteDialog)
        assertEquals(expected = photos, actual = viewModel.state.value.photos)
    }

    @Test
    fun `opening the filter sheet keeps the same photos list`() = runTest {
        // A popup change doesn't restart the database query for the photos.
        val (viewModel, _, fakeRouteRepository) = createViewModel()
        viewModel.onStartTrackPathClick()
        fakeRouteRepository.emitPhotos(RouteId(1), photos)
        val photosBefore = viewModel.state.value.photos

        viewModel.onPlaceFilterClick()

        assertTrue(actual = viewModel.state.value.popups.shouldShowPlaceFilterBottomSheet)
        assertSame(expected = photosBefore, actual = viewModel.state.value.photos)
    }

    @Test
    fun `when tracking stops without a tap on stop then clears the stream and shows no dialog`() = runTest {
        // e.g. the service stops because location updates can't be started
        val (viewModel, fakeRouteTracker, fakeRouteRepository) = createViewModel()
        viewModel.onStartTrackPathClick()
        fakeRouteRepository.emitPhotos(RouteId(1), photos)

        fakeRouteTracker.report(TrackingState.Idle)

        val state = viewModel.state.value
        assertFalse(actual = state.isTracking)
        assertTrue(actual = state.photos.isEmpty())
        assertFalse(actual = state.popups.shouldShowNameRouteDialog)
    }

    @Test
    fun `when confirm in NameRouteDialog is clicked then names the route and clears state`() = runTest {
        val (viewModel, _, fakeRouteRepository) = createViewModel()
        viewModel.onStartTrackPathClick()
        fakeRouteRepository.emitPhotos(RouteId(1), photos)
        viewModel.onStopTrackPathClick()
        viewModel.routeName.setTextAndPlaceCursorAtEnd("  My Cool Route ")

        viewModel.onConfirmNameRouteDialogClick()

        val route = fakeRouteRepository.routes.getValue(RouteId(1))
        assertEquals(expected = "My Cool Route", actual = route.name)
        assertEquals(expected = now, actual = route.finishedAt)

        val state = viewModel.state.value
        assertFalse(actual = state.popups.shouldShowNameRouteDialog)
        assertEquals(expected = "", actual = viewModel.routeName.text.toString())
        assertTrue(actual = state.photos.isEmpty())
        assertTrue(actual = state.popups.shouldShowSnackbarRouteSavedConfirmation)
    }

    @Test
    fun `when confirm is clicked with a blank name then keeps NameRouteDialog open`() = runTest {
        val (viewModel, _, fakeRouteRepository) = createViewModel()
        viewModel.onStartTrackPathClick()
        viewModel.onStopTrackPathClick()
        viewModel.routeName.setTextAndPlaceCursorAtEnd("   ")

        viewModel.onConfirmNameRouteDialogClick()

        assertNull(actual = fakeRouteRepository.routes.getValue(RouteId(1)).name)
        assertTrue(actual = viewModel.state.value.popups.shouldShowNameRouteDialog)
    }

    @Test
    fun `when onHideSnackbarRouteSavedConfirmation is called then resets snackbar state`() = runTest {
        val (viewModel, _, _) = createViewModel()

        viewModel.onStartTrackPathClick()
        viewModel.onStopTrackPathClick()
        viewModel.routeName.setTextAndPlaceCursorAtEnd("My Cool Route")
        // trigger the action that makes the showSnackbarRouteSavedConfirmation true
        viewModel.onConfirmNameRouteDialogClick()

        // check to ensure it is actually true before we hide it
        assertTrue(actual = viewModel.state.value.popups.shouldShowSnackbarRouteSavedConfirmation)

        // call the hide function
        viewModel.hideSnackbarRouteSavedConfirmation()

        assertFalse(actual = viewModel.state.value.popups.shouldShowSnackbarRouteSavedConfirmation)
    }

    @Test
    fun `when discard in NameRouteDialog is clicked then deletes the route and clears state`() = runTest {
        val (viewModel, _, fakeRouteRepository) = createViewModel()
        viewModel.onStartTrackPathClick()
        fakeRouteRepository.emitPhotos(RouteId(1), photos)
        viewModel.onStopTrackPathClick()
        viewModel.routeName.setTextAndPlaceCursorAtEnd("My Cool Route")

        viewModel.onAction(ActivePathViewModel.Action.OnDiscardRouteClick)

        assertTrue(actual = fakeRouteRepository.routes.isEmpty())
        assertEquals(expected = "", actual = viewModel.routeName.text.toString()) // the next route starts empty

        val state = viewModel.state.value
        assertFalse(actual = state.popups.shouldShowNameRouteDialog)
        assertTrue(actual = state.photos.isEmpty())
        assertFalse(actual = state.popups.shouldShowSnackbarRouteSavedConfirmation)
    }

    @Test
    fun `when the name dialog closes then its photos go in the same state update`() = runTest {
        val (viewModel, _, fakeRouteRepository) = createViewModel()
        viewModel.onStartTrackPathClick()
        fakeRouteRepository.emitPhotos(RouteId(1), photos)
        viewModel.onStopTrackPathClick()
        viewModel.routeName.setTextAndPlaceCursorAtEnd("My Cool Route")

        viewModel.state.test {
            assertEquals(expected = photos, actual = awaitItem().photos) // the dialog is open over the photos

            viewModel.onConfirmNameRouteDialogClick()

            // The database query for the photos stops a moment after the dialog closes.
            // No state in between may show the photos of a route that's no longer on screen.
            val closed = awaitItem()
            assertFalse(actual = closed.popups.shouldShowNameRouteDialog)
            assertTrue(actual = closed.photos.isEmpty())
            expectNoEvents()
        }
    }

    @Test
    fun `tracking state reported by the tracker is reflected in viewmodel state`() = runTest {
        val (viewModel, fakeRouteTracker, _) = createViewModel()

        viewModel.state.test {
            assertFalse(actual = awaitItem().isTracking)

            fakeRouteTracker.report(TrackingState.Tracking(RouteId(99)))
            assertTrue(actual = awaitItem().isTracking)

            fakeRouteTracker.report(TrackingState.Idle)
            assertFalse(actual = awaitItem().isTracking)
        }
    }

    @Test
    fun `onPlaceFilterClick shows bottom sheet`() = runTest {
        val (viewModel, _, _) = createViewModel()

        viewModel.onPlaceFilterClick()

        assertTrue(actual = viewModel.state.value.popups.shouldShowPlaceFilterBottomSheet)
    }

    @Test
    fun `given place filter bottom sheet is open when onDismissPlaceFilterBottomSheet then hides bottom sheet`() = runTest {
        val (viewModel, _, _) = createViewModel()

        viewModel.onPlaceFilterClick() // Show it first
        assertTrue(actual = viewModel.state.value.popups.shouldShowPlaceFilterBottomSheet)

        viewModel.onClosePlaceFilterBottomSheet()
        assertFalse(actual = viewModel.state.value.popups.shouldShowPlaceFilterBottomSheet)
    }

    // --- Helper classes and setup ---

    private fun createViewModel(
        routeTrackerFake: RouteTrackerFake = RouteTrackerFake(),
        routeRepositoryFake: RouteRepositoryFake = RouteRepositoryFake()
    ): Triple<ActivePathViewModel, RouteTrackerFake, RouteRepositoryFake> {
        val activePathViewModel = ActivePathViewModel(
            routeTracker = routeTrackerFake,
            startTrackingUseCase = StartTrackingUseCase(routeRepositoryFake, routeTrackerFake),
            stopTrackingUseCase = StopTrackingUseCase(routeTrackerFake, routeRepositoryFake, fixedClock),
            renameRouteUseCase = RenameRouteUseCase(routeRepositoryFake),
            deleteRouteWithPhotoMetadataUseCase = DeleteRouteWithPhotoMetadataUseCase(routeRepositoryFake),
            observeRouteWithPhotoMetadataUseCase = object : ObserveRouteWithPhotoMetadataContract {
                override fun invoke(routeId: RouteId): Flow<RouteWithPhotoMetadata> {
                    return routeRepositoryFake.observeRouteWithPhotoMetadataById(routeId)
                }
            }
        )

        return Triple(activePathViewModel, routeTrackerFake, routeRepositoryFake)
    }

    private fun photo(id: Long) = PhotoMetadata(
        id = id,
        placeId = "place_$id",
        photoUri = "https://example.com/photo_$id.jpg",
        location = Coordinates(latitude = 0.0, longitude = 0.0)
    )

    // Reports what the service would: tracking starts when asked, and stops when asked.
    class RouteTrackerFake(
        initialState: TrackingState = TrackingState.Idle,
        // false: the service reports Idle a moment after Stop, when the test calls report(Idle).
        private val reportsStopRightAway: Boolean = true
    ) : RouteTracker {
        private val _state = MutableStateFlow(initialState)
        override val state: StateFlow<TrackingState> = _state.asStateFlow()

        val startedRouteIds = mutableListOf<RouteId>()
        var stopRequests = 0
            private set

        override fun start(routeId: RouteId) {
            startedRouteIds += routeId
            _state.value = TrackingState.Tracking(routeId)
        }

        override fun stop() {
            stopRequests++
            if (reportsStopRightAway) _state.value = TrackingState.Idle
        }

        fun report(state: TrackingState) {
            _state.value = state
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
        private val photosByRoute = mutableMapOf<RouteId, MutableStateFlow<List<PhotoMetadata>>>()

        // Like the service saving the photos it finds for the route.
        fun emitPhotos(routeId: RouteId, photos: List<PhotoMetadata>) {
            photosOf(routeId).value = photos
        }

        private fun photosOf(routeId: RouteId) = photosByRoute.getOrPut(routeId) { MutableStateFlow(emptyList()) }

        override suspend fun startRoute(): RouteId {
            beforeStartRoute()
            val routeId = RouteId(++lastRouteId)
            routes[routeId] = StoredRoute()
            return routeId
        }

        override suspend fun finishRoute(routeId: RouteId, finishedAt: Instant, metadata: Map<String, String>) {
            // Like the real query: a finished route keeps its first finish time.
            val route = routes[routeId] ?: return
            if (route.finishedAt == null) routes[routeId] = route.copy(finishedAt = finishedAt)
        }

        override suspend fun renameRoute(routeId: RouteId, name: String) {
            routes[routeId]?.let { route -> routes[routeId] = route.copy(name = name) }
        }

        override suspend fun deleteRoute(routeId: RouteId) {
            routes.remove(routeId)
        }

        override fun observeRouteWithPhotoMetadataById(routeId: RouteId): Flow<RouteWithPhotoMetadata> {
            return photosOf(routeId).map { photos ->
                RouteWithPhotoMetadata(
                    routeId = routeId,
                    displayName = "Test",
                    createdAt = Instant.parse("2025-01-01T00:00:00Z"),
                    metadata = emptyMap(),
                    photoMetadata = photos
                )
            }
        }

        override fun observeRoutesWithPhotoMetadata(): Flow<List<RouteWithPhotoMetadata>> {
            return flowOf(emptyList())
        }
    }
}
