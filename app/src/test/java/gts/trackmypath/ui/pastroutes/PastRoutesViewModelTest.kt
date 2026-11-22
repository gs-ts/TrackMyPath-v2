package gts.trackmypath.ui.pastroutes

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import gts.trackmypath.MainDispatcherRule
import gts.trackmypath.domain.route.DeleteRouteWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.ObserveAllRoutesWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.RenameRouteUseCase
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.RouteRepository
import gts.trackmypath.domain.route.RouteWithPhotoMetadata
import gts.trackmypath.ui.pastroutes.PastRoutesViewModel.Action
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Instant

class PastRoutesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val routeRepositoryFake = RouteRepositoryFake(
        route(id = 1, name = "Morning walk"),
        route(id = 2, name = null),
    )

    // Created on first use in a test, after the rule sets Main: viewModelScope keeps the Main it sees first.
    private val viewModel by lazy {
        PastRoutesViewModel(
            observeAllRoutesWithPhotoMetadataUseCase = ObserveAllRoutesWithPhotoMetadataUseCase(routeRepositoryFake),
            renameRouteUseCase = RenameRouteUseCase(routeRepositoryFake),
            deleteRouteWithPhotoMetadataUseCase = DeleteRouteWithPhotoMetadataUseCase(routeRepositoryFake),
        )
    }

    @Test
    fun `shows the routes from the database`() = runTest {
        assertTrue(actual = viewModel.state.value.isLoading) // before the screen collects

        collectState()

        assertFalse(actual = viewModel.state.value.isLoading)
        assertEquals(
            expected = listOf(RouteId(1), RouteId(2)),
            actual = viewModel.state.value.routesWithPhotoMetadata.map { it.routeId },
        )
    }

    @Test
    fun `rename opens the dialog with the route's name, and confirm saves the typed name`() = runTest {
        collectState()

        viewModel.onAction(Action.OnRenameRouteClick(RouteId(1)))
        assertTrue(actual = viewModel.state.value.popups.shouldShowRenameRouteDialog)
        assertEquals(expected = "Morning walk", actual = viewModel.routeName.text.toString())

        viewModel.routeName.setTextAndPlaceCursorAtEnd("  Evening walk ")
        viewModel.onAction(Action.OnConfirmNameRouteDialogClick)

        assertEquals(expected = "Evening walk", actual = routeRepositoryFake.nameOf(RouteId(1)))
        assertEquals(expected = "Evening walk", actual = viewModel.state.value.routesWithPhotoMetadata[0].displayName)
        assertEquals(expected = PastRoutesViewModel.Popups(), actual = viewModel.state.value.popups)
        assertEquals(expected = "", actual = viewModel.routeName.text.toString())
    }

    @Test
    fun `opening a dialog keeps the same routes list, so the list doesn't recompose`() = runTest {
        collectState()
        val routes = viewModel.state.value.routesWithPhotoMetadata

        viewModel.onAction(Action.OnDeleteRouteClick(RouteId(2)))

        assertTrue(actual = viewModel.state.value.popups.shouldShowDeletePastRouteDialog)
        assertSame(expected = routes, actual = viewModel.state.value.routesWithPhotoMetadata)
    }

    @Test
    fun `dismissing the rename dialog keeps the old name`() = runTest {
        collectState()
        viewModel.onAction(Action.OnRenameRouteClick(RouteId(1)))
        viewModel.routeName.setTextAndPlaceCursorAtEnd("Evening walk")

        viewModel.onAction(Action.OnDismissNameRouteDialogClick)

        assertEquals(expected = "Morning walk", actual = routeRepositoryFake.nameOf(RouteId(1)))
        assertEquals(expected = PastRoutesViewModel.Popups(), actual = viewModel.state.value.popups)
        assertEquals(expected = "", actual = viewModel.routeName.text.toString())
    }

    @Test
    fun `delete asks first, then deletes the route and shows the snackbar`() = runTest {
        collectState()

        viewModel.onAction(Action.OnDeleteRouteClick(RouteId(1)))
        assertTrue(actual = viewModel.state.value.popups.shouldShowDeletePastRouteDialog)
        assertEquals(expected = 2, actual = viewModel.state.value.routesWithPhotoMetadata.size)

        viewModel.onAction(Action.OnConfirmDeleteRouteClick)

        assertEquals(
            expected = listOf(RouteId(2)),
            actual = viewModel.state.value.routesWithPhotoMetadata.map { it.routeId },
        )
        assertFalse(actual = viewModel.state.value.popups.shouldShowDeletePastRouteDialog)
        assertTrue(actual = viewModel.state.value.popups.shouldShowSnackbarRouteDeletedConfirmation)

        viewModel.onAction(Action.HideSnackbarRouteDeletedConfirmation)
        assertFalse(actual = viewModel.state.value.popups.shouldShowSnackbarRouteDeletedConfirmation)
    }

    @Test
    fun `dismissing the delete dialog keeps the route`() = runTest {
        collectState()
        viewModel.onAction(Action.OnDeleteRouteClick(RouteId(1)))

        viewModel.onAction(Action.OnDismissDeleteRouteDialogClick)

        assertEquals(expected = 2, actual = viewModel.state.value.routesWithPhotoMetadata.size)
        assertEquals(expected = PastRoutesViewModel.Popups(), actual = viewModel.state.value.popups)
    }

    @Test
    fun `stops observing the database 5 s after the screen goes away, and keeps the last state`() = runTest {
        // e.g. the user opens a route's details
        val screen = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        assertEquals(expected = 1, actual = routeRepositoryFake.observers)

        screen.cancel()
        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(expected = 1, actual = routeRepositoryFake.observers)

        advanceTimeBy(2)
        runCurrent()
        assertEquals(expected = 0, actual = routeRepositoryFake.observers)
        assertEquals(expected = 2, actual = viewModel.state.value.routesWithPhotoMetadata.size)
    }

    // Like the screen: state.value is current only while something collects the state.
    private fun TestScope.collectState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    }

    private fun route(id: Long, name: String?) = RouteWithPhotoMetadata(
        routeId = RouteId(id),
        displayName = name,
        createdAt = Instant.parse("2026-11-18T10:00:00Z"),
        metadata = emptyMap(),
        photoMetadata = emptyList(),
    )

    // Like the database: a rename or a delete updates the routes that the screen observes.
    private class RouteRepositoryFake(vararg routes: RouteWithPhotoMetadata) : RouteRepository {
        private val routes = MutableStateFlow(routes.toList())

        val observers: Int
            get() = routes.subscriptionCount.value

        fun nameOf(routeId: RouteId): String? = routes.value.single { it.routeId == routeId }.displayName

        override fun observeRoutesWithPhotoMetadata(): Flow<List<RouteWithPhotoMetadata>> = routes

        override suspend fun renameRoute(routeId: RouteId, name: String) {
            routes.update { routes ->
                routes.map { route -> if (route.routeId == routeId) route.copy(displayName = name) else route }
            }
        }

        override suspend fun deleteRoute(routeId: RouteId) {
            routes.update { routes -> routes.filterNot { it.routeId == routeId } }
        }

        override suspend fun startRoute(): RouteId = error("Not used by Past Routes")

        override suspend fun finishRoute(routeId: RouteId, finishedAt: Instant, metadata: Map<String, String>) =
            error("Not used by Past Routes")

        override fun observeRouteWithPhotoMetadataById(routeId: RouteId): Flow<RouteWithPhotoMetadata> =
            error("Not used by Past Routes")
    }
}
