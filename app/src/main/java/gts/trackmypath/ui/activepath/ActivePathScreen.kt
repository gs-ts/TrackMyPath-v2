package gts.trackmypath.ui.activepath

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.POST_NOTIFICATIONS
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalAsyncImagePreviewHandler
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import gts.trackmypath.R
import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.ui.composables.NameRouteDialog
import gts.trackmypath.ui.composables.PhotoCard
import gts.trackmypath.ui.mockdata.photoMetadataMock
import gts.trackmypath.ui.mockdata.previewHandler
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

    ActivePathContent(
        state = state,
        onAction = viewModel::onAction,
        onNavigateToPastRoutes = onNavigateToPastRoutes
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
private fun ActivePathContent(
    modifier: Modifier = Modifier,
    state: ActivePathViewModel.State,
    onAction: (ActivePathViewModel.Action) -> Unit,
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
            if (state.isTracking) {
                onAction(ActivePathViewModel.Action.OnStopTrackPathClick)
            } else {
                onAction(ActivePathViewModel.Action.OnStartTrackPathClick)
            }
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
            onRouteNameChange = { onAction(ActivePathViewModel.Action.OnRouteNameChange(routeName = it)) },
            onConfirmClick = { onAction(ActivePathViewModel.Action.OnConfirmNameRouteDialogClick) },
            onDismissClick = { onAction(ActivePathViewModel.Action.OnDismissNameRouteDialogClick) }
        )
    }

    // hideSnackbarRouteSavedConfirmation ensures the LaunchedEffect always calls the
    // most up-to-date version of the lambda without having to restart the effect itself.
    // explanation:
    // https://mrmans0n.github.io/compose-rules/rules/#be-mindful-of-the-arguments-you-use-inside-of-a-restarting-effect
    val hideSnackbarRouteSavedConfirmation by rememberUpdatedState(
        newValue = {
            onAction(ActivePathViewModel.Action.HideSnackbarRouteSavedConfirmation)
        }
    )
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(key1 = state.showSnackbarRouteSavedConfirmation) {
        if (state.showSnackbarRouteSavedConfirmation) {
            try {
                snackbarHostState.showSnackbar(
                    message = "Route saved successfully.",
                    duration = SnackbarDuration.Short
                )
            } finally {
                // If the snackbar finishes naturally, it clears the state.
                // If the user navigates away and cancels the coroutine, it ALSO clears the state!
                hideSnackbarRouteSavedConfirmation()
            }
        }
    }

    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    if (state.showPlaceFilterBottomSheet) {
        PlaceFilterBottomSheet(
            sheetState = sheetState,
            onClose = { onAction(ActivePathViewModel.Action.OnClosePlaceFilterBottomSheet) }
        )
    }

    Scaffold(
        // https://developer.android.com/develop/ui/compose/testing/interoperability
        modifier = modifier.semantics { testTagsAsResourceId = true },
        topBar = {
            ActivePathTopAppBar(
                onAction = onAction,
                onNavigateToPastRoutes = onNavigateToPastRoutes
            )
        },
        floatingActionButton = {
            TrackFloatingActionButton(
                isTracking = state.isTracking,
                locationPermissionsState = locationPermissionsState,
                onPermissionRequest = { locationPermissionDialogType = it },
                onAction = onAction
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
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
private fun ActivePathTopAppBar(
    onAction: (ActivePathViewModel.Action) -> Unit,
    onNavigateToPastRoutes: () -> Unit
) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = "track my path")
        },
        actions = {
            IconButton(onClick = { onAction(ActivePathViewModel.Action.OnPlaceFiltersClick) }) {
                Icon(
                    painter = painterResource(R.drawable.place_filter_icon),
                    contentDescription = "Filter places"
                )
            }
            IconButton(onClick = onNavigateToPastRoutes) {
                Icon(
                    painter = painterResource(R.drawable.routes_icon),
                    contentDescription = "Past Routes"
                )
            }
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun TrackFloatingActionButton(
    isTracking: Boolean,
    locationPermissionsState: MultiplePermissionsState,
    onPermissionRequest: (LocationPermissionDialogType) -> Unit,
    onAction: (ActivePathViewModel.Action) -> Unit
) {
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
                    if (isTracking) {
                        onAction(ActivePathViewModel.Action.OnStopTrackPathClick)
                    } else {
                        onAction(ActivePathViewModel.Action.OnStartTrackPathClick)
                    }
                }

                isCoarseGranted -> {
                    // They have coarse. Show dialog to upgrade to fine.
                    onPermissionRequest(LocationPermissionDialogType.UPGRADE_TO_FINE)
                }

                else -> {
                    // No permissions granted yet.
                    if (locationPermissionsState.shouldShowRationale) {
                        onPermissionRequest(LocationPermissionDialogType.REQUIRE_ALL)
                    } else {
                        locationPermissionsState.launchMultiplePermissionRequest()
                    }
                }
            }
        },
    ) {
        val icon = if (isTracking) {
            painterResource(R.drawable.stop_icon)
        } else {
            painterResource(R.drawable.play_arrow_icon)
        }
        val contentDescription = if (isTracking) {
            "Stop path tracking"
        } else {
            "Start path tracking"
        }
        Icon(
            painter = icon,
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun EmptyStream(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(32.dp),
            painter = painterResource(R.drawable.walk_icon),
            contentDescription = "No photos",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No photos captured yet. Start tracking to see photos taken along your route.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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

private enum class LocationPermissionDialogType {
    NONE,
    REQUIRE_ALL,
    UPGRADE_TO_FINE
}

@OptIn(ExperimentalCoilApi::class)
@PreviewLightDark
@Composable
private fun ActivePathStartedPreview() {
    TrackMyPathV2Theme {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
            ActivePathContent(
                state = ActivePathViewModel.State(
                    isLocationServiceRunning = true,
                    ongoingRouteId = RouteId(1),
                    photos = photoMetadataMock
                ),
                onAction = {},
                onNavigateToPastRoutes = {}
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ActivePathStoppedPreview() {
    TrackMyPathV2Theme {
        ActivePathContent(
            state = ActivePathViewModel.State(photos = persistentListOf()),
            onAction = {},
            onNavigateToPastRoutes = {}
        )
    }
}
