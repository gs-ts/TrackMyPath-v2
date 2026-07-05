package gts.trackmypath.ui.activepath

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.rememberViewModelStoreOwner
import gts.trackmypath.domain.filters.PlaceFilter
import gts.trackmypath.ui.theme.TrackMyPathV2Theme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Suppress("ViewModelInjection")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceFilterBottomSheet(
    sheetState: SheetState,
    onClose: () -> Unit
) {
    val bottomSheetStoreOwner = rememberViewModelStoreOwner()
    // explanation here:
    // https://mrmans0n.github.io/compose-rules/rules/#be-mindful-of-the-arguments-you-use-inside-of-a-restarting-effect
    val currentOnClose by rememberUpdatedState(onClose)

    CompositionLocalProvider(value = LocalViewModelStoreOwner provides bottomSheetStoreOwner) {
        val viewModel: PlaceFilterViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val placeFilters = PlaceFilter.entries.toList().toPersistentList()

        LaunchedEffect(state.isBottomSheetDismissed) {
            if (state.isBottomSheetDismissed) {
                currentOnClose()
            }
        }

        ModalBottomSheet(
            onDismissRequest = viewModel::onClose,
            sheetState = sheetState
        ) {

            PlaceFilterBottomSheetContent(
                state = state,
                placeFilters = placeFilters,
                onPlaceFilterSelect = viewModel::onPlaceFilterSelect,
                onResetPlaceFiltersClick = viewModel::onResetPlaceFiltersClick
            )
        }
    }
}

@Composable
private fun PlaceFilterBottomSheetContent(
    state: PlaceFilterViewModel.State,
    placeFilters: ImmutableList<PlaceFilter>,
    onPlaceFilterSelect: (PlaceFilter) -> Unit,
    onResetPlaceFiltersClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Filter Places",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            placeFilters.forEach { placeFilter ->
                FilterChip(
                    selected = state.selectedPlaceFilters.contains(placeFilter),
                    onClick = { onPlaceFilterSelect(placeFilter) },
                    label = { Text(text = placeFilter.title) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onResetPlaceFiltersClick) {
                Text(text = "Reset")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun PlaceFilterBottomSheetPreview() {
    TrackMyPathV2Theme {
        Column(modifier = Modifier.fillMaxSize()) {
            PlaceFilterBottomSheetContent(
                state = PlaceFilterViewModel.State(),
                placeFilters = PlaceFilter.entries.toList().toPersistentList(),
                onPlaceFilterSelect = {},
                onResetPlaceFiltersClick = {}
            )
        }
    }
}
