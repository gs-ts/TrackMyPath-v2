package gts.trackmypath.ui.activepath

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.compose.SubcomposeAsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import gts.trackmypath.R
import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.ui.activepath.ActivePathViewModel.State.TrackingState
import gts.trackmypath.ui.mockdata.photoMetadataMock
import gts.trackmypath.ui.mockdata.previewHandler
import gts.trackmypath.ui.service.LocationService
import gts.trackmypath.ui.theme.TrackMyPathV2Theme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ActivePathScreen(
    viewModel: ActivePathViewModel,
    onNavigateToPastRoutes: () -> Unit
) {

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
        onDismissNameRouteDialogClick = viewModel::onDismissNameRouteDialogClick,
        onNavigateToPastRoutes = onNavigateToPastRoutes
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
    onDismissNameRouteDialogClick: () -> Unit,
    onNavigateToPastRoutes: () -> Unit
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
        // https://developer.android.com/develop/ui/compose/testing/interoperability
        modifier = modifier.semantics { testTagsAsResourceId = true },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "track my path")
                },
                actions = {
                    IconButton(onClick = onNavigateToPastRoutes) {
                        Icon(
                            painter = painterResource(R.drawable.routes_icon),
                            contentDescription = "Past Routes"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.testTag("start_tracking_fab"),
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
        if (state.photos.isEmpty()) {
            EmptyStream(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp)
            )
        }
        PhotoStream(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            photos = state.photos
        )
    }
}

@Composable
private fun EmptyStream(modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(32.dp),
            painter = painterResource(R.drawable.walk_icon),
            contentDescription = "No photos",
            tint = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No photos captured yet. Start tracking to see photos taken along your route.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun PhotoStream(
    modifier: Modifier = Modifier,
    photos: ImmutableList<PhotoMetadata>
) {
    val listState = rememberLazyListState()

    LaunchedEffect(photos.size) {
        if (photos.isNotEmpty()) {
            listState.animateScrollToItem(index = 0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.testTag("photo_list_scrollable"),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        items(
            items = photos,
            key = { photo -> photo.id },
            contentType = { "photo_item" }
        ) { photo ->
            PhotoCard(photo = photo)
        }
    }
}

@Composable
private fun PhotoCard(photo: PhotoMetadata) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(top = 8.dp)
                .padding(bottom = 16.dp)
        ) {
            SubcomposeAsyncImage(
                model = photo.photoUri,
                contentDescription = photo.generativeSummary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(vertical = 4.dp),
                contentScale = ContentScale.Crop,
                loading = {
                    Box(modifier = Modifier.fillMaxSize().shimmer())
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (photo.displayName != null) {
                Text(
                    text = photo.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = Ellipsis,
                )
            } else {
                Text(
                    text = "no description available",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

private enum class LocationPermissionDialogType {
    NONE,
    REQUIRE_ALL,
    UPGRADE_TO_FINE
}

@OptIn(ExperimentalCoilApi::class)
@Preview(showSystemUi = true)
@Composable
private fun ActivePathStartedPreview() {
    TrackMyPathV2Theme {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
            ActivePathContent(
                state = ActivePathViewModel.State(photos = photoMetadataMock),
                trackingState = TrackingState.STARTED,
                onTrackPathClick = {},
                onRouteNameChange = {},
                onConfirmNameRouteDialogClick = {},
                onDismissNameRouteDialogClick = {},
                onNavigateToPastRoutes = {}
            )
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun ActivePathStoppedPreview() {
    TrackMyPathV2Theme {
        ActivePathContent(
            state = ActivePathViewModel.State(photos = persistentListOf()),
            trackingState = TrackingState.STOPPED,
            onTrackPathClick = {},
            onRouteNameChange = {},
            onConfirmNameRouteDialogClick = {},
            onDismissNameRouteDialogClick = {},
            onNavigateToPastRoutes = {}
        )
    }
}
