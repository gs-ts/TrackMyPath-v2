package gts.trackmypath.ui.activepath

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.ui.activepath.ActivePathViewModel.State.TrackingState
import gts.trackmypath.ui.service.LocationService
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ActivePathScreen(viewModel: ActivePathViewModel) {

    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(key1 = state.trackingState) {
        val locationServiceIntent = Intent(context, LocationService::class.java)

        when (state.trackingState) {
            TrackingState.STARTED -> {
                state.ongoingRouteId?.let { routeId ->
                    locationServiceIntent.putExtra("EXTRA_ROUTE_ID", routeId.id)
                }
                context.startService(locationServiceIntent)
            }
            TrackingState.STOPPED -> {
                context.stopService(locationServiceIntent)
            }
        }
    }

    ActivePathContent(
        state = state,
        trackingState = state.trackingState,
        onTrackPathClick = viewModel::onTrackPathClick,
        onRouteNameChange = viewModel::onRouteNameChange,
        onConfirmNameRouteDialogClick = viewModel::onConfirmNameRouteDialogClick,
        onDismissNameRouteDialogClick = viewModel::onDismissNameRouteDialogClick
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun ActivePathContent(
    modifier: Modifier = Modifier,
    state: ActivePathViewModel.State,
    trackingState: TrackingState,
    onTrackPathClick: () -> Unit,
    onRouteNameChange: (String) -> Unit,
    onConfirmNameRouteDialogClick: () -> Unit,
    onDismissNameRouteDialogClick: () -> Unit
) {
    var locationPermissionDialogType by remember { mutableStateOf(LocationPermissionDialogType.NONE) }

    val postNotificationPermission = rememberPermissionState(permission = POST_NOTIFICATIONS)
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

    if (locationPermissionDialogType != LocationPermissionDialogType.NONE) {
        LocationPermissionRequestDialog(
            isUpgradeFromCoarseToFine = locationPermissionDialogType == LocationPermissionDialogType.UPGRADE_TO_FINE,
            onConfirmClick = {
                locationPermissionDialogType = LocationPermissionDialogType.NONE
                locationPermissionsState.launchMultiplePermissionRequest()
            },
            onDismissRequest = {
                // if user refuses precise, they cannot track.
                locationPermissionDialogType = LocationPermissionDialogType.NONE
            },
        )
    }

    if (state.showNameRouteDialog) {
        NameRouteDialog(
            routeName = state.routeNameInput,
            onRouteNameChange = onRouteNameChange,
            onConfirmClick = onConfirmNameRouteDialogClick,
            onDismissClick = onDismissNameRouteDialogClick
        )
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
                    val isFineGranted = locationPermissionsState.permissions.any {
                        it.permission == ACCESS_FINE_LOCATION && it.status.isGranted
                    }
                    val isCoarseGranted = locationPermissionsState.permissions.any {
                        it.permission == ACCESS_COARSE_LOCATION && it.status.isGranted
                    }

                    when {
                        isFineGranted -> {
                            onTrackPathClick()
                        }

                        isCoarseGranted -> {
                            // They have coarse. Show dialog to upgrade to fine.
                            locationPermissionDialogType = LocationPermissionDialogType.UPGRADE_TO_FINE
                        }

                        else -> {
                            // No permissions granted yet.
                            if (locationPermissionsState.shouldShowRationale) {
                                locationPermissionDialogType = LocationPermissionDialogType.REQUIRE_ALL
                            } else {
                                locationPermissionsState.launchMultiplePermissionRequest()
                            }
                        }
                    }
                },
            ) {
                val icon = if (trackingState == TrackingState.STOPPED) {
                    painterResource(R.drawable.play_arrow_icon)
                } else {
                    painterResource(R.drawable.stop_icon)
                }
                val contentDescription = if (trackingState == TrackingState.STOPPED) {
                    "Start path tracking"
                } else {
                    "Stop path tracking"
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
            photos = state.photos.toImmutableList()
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
                model = photo.photoUri,
                contentDescription = "image of pokemon",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
//                placeholder = painterResource(id = R.drawable.loading),
            )
        }
    }
}

private enum class LocationPermissionDialogType {
    NONE,
    REQUIRE_ALL,
    UPGRADE_TO_FINE
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview(showSystemUi = true)
@Composable
private fun ActivePathPreview() {
    ActivePathContent(
        state = ActivePathViewModel.State(
            photos = persistentListOf(),
        ),
        trackingState = TrackingState.STOPPED,
        onTrackPathClick = {},
        onRouteNameChange = {},
        onConfirmNameRouteDialogClick = {},
        onDismissNameRouteDialogClick = {}
    )
}
