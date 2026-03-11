package gts.trackmypath.ui

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
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
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator()
        ),
        entryProvider = { key ->
            when (key) {
                FeedRoute -> {
                    NavEntry(key = key) {
                        ActivePathScreen(viewModel = hiltViewModel())
                    }
                }
                PastPathsRoute -> {
                    NavEntry(key = key) {
                        PastPathsScreen()
                    }
                }
                else -> {
                    throw RuntimeException("Invalid NavKey")
                }
            }
        },
    )
}
