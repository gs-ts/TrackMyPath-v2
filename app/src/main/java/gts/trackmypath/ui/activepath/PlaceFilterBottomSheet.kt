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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import gts.trackmypath.domain.PlaceFilter
import gts.trackmypath.ui.theme.TrackMyPathV2Theme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceFilterBottomSheet(
    sheetState: SheetState,
    placeFilters: ImmutableList<PlaceFilter>,
    selectedPlaceFilters: ImmutableSet<PlaceFilter>,
    onPlaceFilterSelect: (PlaceFilter) -> Unit,
    onResetPlaceFiltersClick: () -> Unit,
    onDismissRequest: () -> Unit
) {

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
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
                        selected = selectedPlaceFilters.contains(placeFilter),
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
}

@OptIn(ExperimentalMaterial3Api::class)
@PreviewLightDark
@Composable
private fun PlaceFilterBottomSheetPreview() {
    TrackMyPathV2Theme {
        Column(modifier = Modifier.fillMaxSize()) {
            PlaceFilterBottomSheet(
                sheetState = rememberModalBottomSheetState(),
                placeFilters = PlaceFilter.entries.toList().toPersistentList(),
                selectedPlaceFilters = persistentSetOf(PlaceFilter.FOOD_AND_DRINKS),
                onPlaceFilterSelect = {},
                onResetPlaceFiltersClick = {},
                onDismissRequest = {}
            )
        }
    }
}
