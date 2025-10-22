package gts.trackmypath.ui.activepath

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import gts.trackmypath.R
import gts.trackmypath.domain.PhotoMetadata
import gts.trackmypath.ui.LocationService
import gts.trackmypath.ui.activepath.ActivePathViewModel.State.TrackingState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ActivePathScreen(viewModel: ActivePathViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ActivePathContent(
        photos = state.photos.toImmutableList(),
        trackingState = state.trackingState,
        onTrackPathClick = viewModel::onTrackPathClick,
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun ActivePathContent(
    modifier: Modifier = Modifier,
    photos: ImmutableList<PhotoMetadata>,
    trackingState: TrackingState,
    onTrackPathClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var shouldShowLocationPermissionRationaleRequestDialog by remember { mutableStateOf(false) }

    val postNotificationPermission =
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    LaunchedEffect(key1 = true) {
        if (!postNotificationPermission.status.isGranted) {
            postNotificationPermission.launchPermissionRequest()
        }
    }

    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION, POST_NOTIFICATIONS),
    ) { permissions ->
        if (permissions[ACCESS_FINE_LOCATION] == true && permissions[POST_NOTIFICATIONS] == true) {
            onTrackPathClick()
        }
    }

    if (shouldShowLocationPermissionRationaleRequestDialog) {
        LocationPermissionRequestDialog(
            onConfirmClick = {
                shouldShowLocationPermissionRationaleRequestDialog = false
                locationPermissionsState.launchMultiplePermissionRequest()
            },
            onDismissRequest = {
                shouldShowLocationPermissionRationaleRequestDialog = false
            },
        )
    }

    val locationService = remember { Intent(context, LocationService::class.java) }
    LaunchedEffect(trackingState) {
        if (trackingState == TrackingState.TRACKING) {
            context.startService(locationService)
        } else {
            context.stopService(locationService)
        }
    }

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
            FloatingActionButton(
                onClick = {
                    val isFineLocationGranted = locationPermissionsState.permissions.find {
                        it.permission == ACCESS_FINE_LOCATION
                    }?.status?.isGranted == true

                    val isCoarseLocationGranted = locationPermissionsState.permissions.find {
                        it.permission == ACCESS_COARSE_LOCATION
                    }?.status?.isGranted == true

                    if (isFineLocationGranted) {
                        onTrackPathClick()
                    } else if (isCoarseLocationGranted) {
                        // Inform the user that fine location provides better results.
                        shouldShowLocationPermissionRationaleRequestDialog = true
                    } else {
                        shouldShowLocationPermissionRationaleRequestDialog = true
                    }
                },
            ) {
                val icon = if (trackingState == TrackingState.TRACKING) {
                    painterResource(R.drawable.stop_icon)
                } else {
                    painterResource(R.drawable.play_arrow_icon)
                }
                val contentDescription = if (trackingState == TrackingState.TRACKING) {
                    "Stop path tracking"
                } else {
                    "Start path tracking"
                }
                Icon(
                    painter = icon,
                    contentDescription = contentDescription,
                )
            }
        },
    ) { innerPadding ->
        PhotoStream(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(12.dp),
            photos = photos
        )
    }
}

@Composable
private fun PhotoStream(
    modifier: Modifier = Modifier,
    photos: ImmutableList<PhotoMetadata>
) {
    LazyColumn(modifier = modifier) {
        items(
            items = photos,
            key = { photo -> photo.id }
        ) { photo ->
            AsyncImage(
                model = photo.photoUri.toString(),
                contentDescription = "image of pokemon",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
//                placeholder = painterResource(id = R.drawable.loading),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationPermissionRequestDialog(
    onConfirmClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    BasicAlertDialog(
        content = {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Use precise location for better results.")
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        TextButton(
                            onClick = onDismissRequest,
                        ) {
                            Text("Dismiss")
                        }
                        TextButton(
                            onClick = onConfirmClick,
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismissRequest,
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Preview
@Composable
private fun ActivePathPreview() {
    ActivePathContent(
        photos = persistentListOf(),
        trackingState = TrackingState.STOPPED
    )
}
