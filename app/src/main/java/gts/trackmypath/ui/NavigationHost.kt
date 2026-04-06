package gts.trackmypath.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import gts.trackmypath.ui.activepath.ActivePathScreen
import gts.trackmypath.ui.pastroutes.PastRouteDetailScreen
import gts.trackmypath.ui.pastroutes.PastRouteDetailViewModel
import gts.trackmypath.ui.pastroutes.PastRoutesScreen
import kotlinx.serialization.Serializable

@Serializable
data object ActivePathRoute : NavKey

@Serializable
data object PastRoutesRoute : NavKey

@Serializable
data class PastRouteDetailRoute(val routeId: Long) : NavKey

@Composable
fun NavigationHost() {
    val backStack = rememberNavBackStack(ActivePathRoute)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<ActivePathRoute> {
                ActivePathScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToPastRoutes = {
                        backStack.add(PastRoutesRoute)
                    }
                )
            }
            entry<PastRoutesRoute> {
                PastRoutesScreen(
                    viewModel = hiltViewModel(),
                    onNavigateToPastRouteDetail = { routeId ->
                        backStack.add(PastRouteDetailRoute(routeId = routeId.id))
                    },
                    onBackClick = {
                        backStack.removeLastOrNull()
                    }
                )
            }
            @Suppress("ViewModelInjection")
            // based on https://developer.android.com/guide/navigation/navigation-3/recipes/passingarguments
            entry<PastRouteDetailRoute> { navKey ->
                val viewModel = hiltViewModel<PastRouteDetailViewModel, PastRouteDetailViewModel.Factory>(
                    creationCallback = { factory ->
                        factory.create(navKey = navKey)
                    }
                )
                PastRouteDetailScreen(
                    viewModel = viewModel,
                    onBackClick = {
                        backStack.removeLastOrNull()
                    }
                )
            }
        },
        transitionSpec = {
            slideInHorizontally(
                initialOffsetX = { it }
            ) togetherWith
                slideOutHorizontally(targetOffsetX = { -it })
        },
        popTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
        predictivePopTransitionSpec = {
            slideInHorizontally(initialOffsetX = { -it }) togetherWith
                slideOutHorizontally(targetOffsetX = { it })
        },
    )
}
