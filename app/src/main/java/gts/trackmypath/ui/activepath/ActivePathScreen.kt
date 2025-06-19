package gts.trackmypath.ui.activepath

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gts.trackmypath.ui.activepath.ActivePathViewModel.State.TrackingState

@Composable
fun ActivePathScreen(viewModel: ActivePathViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ActivePathContent(
        trackingState = state.trackingState,
        onTrackPathClick = viewModel::onTrackPathClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivePathContent(
    modifier: Modifier = Modifier,
    trackingState: TrackingState,
    onTrackPathClick: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text("Path")
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onTrackPathClick) {
                val icon = if (trackingState == TrackingState.TRACKING) {
                    Icons.Default.Stop
                } else {
                    Icons.Default.PlayArrow
                }
                val contentDescription = if (trackingState == TrackingState.TRACKING) {
                    "Stop path tracking"
                } else {
                    "Start path tracking"
                }
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
        ) {
            Text("hi there!")
        }
    }
}
