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
import gts.trackmypath.ui.pastroutes.PastRoutesScreen
import kotlinx.serialization.Serializable

@Serializable
data object FeedRoute : NavKey

@Serializable
data object PastRoutesRoute : NavKey

@Composable
fun NavigationHost() {
    val backStack = rememberNavBackStack(FeedRoute)

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeAt(backStack.lastIndex)
            }
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<FeedRoute> {
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
                    onBackClick = {
                        backStack.removeAt(backStack.lastIndex)
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
