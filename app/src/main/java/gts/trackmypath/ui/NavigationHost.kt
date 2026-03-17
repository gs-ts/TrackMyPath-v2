package gts.trackmypath.ui

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import gts.trackmypath.ui.activepath.ActivePathScreen
import gts.trackmypath.ui.pastpaths.PastPathsScreen
import kotlinx.serialization.Serializable

@Serializable
data object FeedRoute : NavKey

@Serializable
data object PastPathsRoute : NavKey

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
                ActivePathScreen(viewModel = hiltViewModel())
            }
            entry<PastPathsRoute> {
                PastPathsScreen()
            }
        }
    )
}
