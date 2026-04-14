@file:OptIn(ExperimentalCoilApi::class)

package gts.trackmypath.ui.pastroutes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.LocalAsyncImagePreviewHandler
import coil3.compose.SubcomposeAsyncImage
import gts.trackmypath.R
import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.ui.composables.LoadingView
import gts.trackmypath.ui.composables.shimmer
import gts.trackmypath.ui.mockdata.previewHandler
import gts.trackmypath.ui.mockdata.routesWithPhotoMetadataMock
import gts.trackmypath.ui.model.RouteWithPhotoMetadataUiState
import gts.trackmypath.ui.theme.TrackMyPathV2Theme
import kotlinx.collections.immutable.ImmutableList

@Composable
fun PastRoutesScreen(
    viewModel: PastRoutesViewModel,
    onNavigateToPastRouteDetail: (RouteId) -> Unit,
    onBackClick: () -> Unit
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.isLoading) {
        LoadingView()
    } else {
        PastRoutesContent(
            state = state,
//            routesWithPhotoMetadata = state.routesWithPhotoMetadata,
//            showDeletePastRouteDialog = state.showDeletePastRouteDialog,
            onRouteCardClick = onNavigateToPastRouteDetail,
            onDeleteRouteClick = viewModel::onDeleteRouteClick,
            onConfirmDeleteRouteClick = viewModel::onConfirmDeleteRouteClick,
            onDismissDeleteRouteDialogClick = viewModel::onDismissDeleteRouteDialogClick,
            onHideSnackbarRouteDeletedConfirmation = viewModel::onHideSnackbarRouteDeletedConfirmation,
            onBackClick = onBackClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastRoutesContent(
    modifier: Modifier = Modifier,
    state: PastRoutesViewModel.State,
    onRouteCardClick: (RouteId) -> Unit,
    onDeleteRouteClick: (RouteId) -> Unit,
    onConfirmDeleteRouteClick: () -> Unit,
    onDismissDeleteRouteDialogClick: () -> Unit,
    onHideSnackbarRouteDeletedConfirmation: () -> Unit,
    onBackClick: () -> Unit,
) {
    if (state.showDeletePastRouteDialog) {
        DeletePastRouteDialog(
            onConfirmClick = onConfirmDeleteRouteClick,
            onDismissClick = onDismissDeleteRouteDialogClick
        )
    }

    // hideSnackbarRouteSavedConfirmation ensures the LaunchedEffect always calls the
    // most up-to-date version of the lambda without having to restart the effect itself.
    // explanation:
    // https://mrmans0n.github.io/compose-rules/rules/#be-mindful-of-the-arguments-you-use-inside-of-a-restarting-effect
    val hideSnackbarRouteDeletedConfirmation by rememberUpdatedState(newValue = onHideSnackbarRouteDeletedConfirmation)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(key1 = state.showSnackbarRouteDeletedConfirmation) {
        if (state.showSnackbarRouteDeletedConfirmation) {
            try {
                snackbarHostState.showSnackbar(
                    message = "Route deleted successfully.",
                    duration = SnackbarDuration.Short
                )
            } finally {
                // If the snackbar finishes naturally, it clears the state.
                // If the user navigates away and cancels the coroutine, it ALSO clears the state!
                hideSnackbarRouteDeletedConfirmation()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "past routes")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back_icon),
                            contentDescription = "navigate back"
                        )
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        if (state.routesWithPhotoMetadata.isNotEmpty()) {
            PastRoutesList(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                routesWithPhotoMetadata = state.routesWithPhotoMetadata,
                onRouteCardClick = onRouteCardClick,
                onDeleteRouteClick = onDeleteRouteClick
            )
        } else {
            EmptyPastRoutes(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp)
            )
        }
    }
}

@Composable
private fun PastRoutesList(
    modifier: Modifier = Modifier,
    routesWithPhotoMetadata: ImmutableList<RouteWithPhotoMetadataUiState>,
    onRouteCardClick: (RouteId) -> Unit,
    onDeleteRouteClick: (RouteId) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp)
    ) {
        items(
            items = routesWithPhotoMetadata,
            key = { routeWithPhotoMetadata -> routeWithPhotoMetadata.routeId.id }
        ) { routeWithPhotoMetadata ->
            RouteCard(
                modifier = Modifier.animateItem(),
                routeWithPhotoMetadata = routeWithPhotoMetadata,
                onRouteCardClick = onRouteCardClick,
                onDeleteClick = onDeleteRouteClick
            )
        }
    }
}

@Composable
private fun EmptyPastRoutes(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(32.dp),
            painter = painterResource(R.drawable.walk_icon),
            contentDescription = "No past routes",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "No past routes found.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RouteCard(
    modifier: Modifier = Modifier,
    routeWithPhotoMetadata: RouteWithPhotoMetadataUiState,
    onRouteCardClick: (RouteId) -> Unit,
    onDeleteClick: (RouteId) -> Unit
) {
    @Suppress("MagicNumber")
    val numberOfPreviewPhotos = 3
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = { onRouteCardClick(routeWithPhotoMetadata.routeId) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                    text = routeWithPhotoMetadata.displayName.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                IconButton(
                    onClick = { onDeleteClick(routeWithPhotoMetadata.routeId) }
                ) {
                    Icon(
                        modifier = Modifier.size(20.dp),
                        painter = painterResource(R.drawable.delete_icon),
                        contentDescription = "Delete route",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val photos = routeWithPhotoMetadata.photoMetadata.take(numberOfPreviewPhotos)

                photos.forEach { photo ->
                    PhotoPreview(
                        photo = photo,
                        modifier = Modifier.weight(1f)
                    )
                }

                // if less than 3 photos, weight(1f) will make them stretch across the whole screen
                repeat(numberOfPreviewPhotos - photos.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier.padding(start = 4.dp),
                text = routeWithPhotoMetadata.createdAt,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PhotoPreview(
    modifier: Modifier = Modifier,
    photo: PhotoMetadata
) {
    SubcomposeAsyncImage(
        model = photo.photoUri,
        contentDescription = photo.generativeSummary,
        modifier = modifier
            .aspectRatio(1f)
            .clip(shape = RoundedCornerShape(size = 8.dp)),
        contentScale = ContentScale.Crop,
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shimmer()
            )
        }
    )
}

@PreviewLightDark
@Composable
private fun PastRoutesPreview() {
    TrackMyPathV2Theme {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
            PastRoutesContent(
                state = PastRoutesViewModel.State(routesWithPhotoMetadata = routesWithPhotoMetadataMock),
                onRouteCardClick = {},
                onDeleteRouteClick = {},
                onConfirmDeleteRouteClick = {},
                onDismissDeleteRouteDialogClick = {},
                onHideSnackbarRouteDeletedConfirmation = {},
                onBackClick = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun RouteCardPreview() {
    TrackMyPathV2Theme {
        CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
            RouteCard(
                routeWithPhotoMetadata = routesWithPhotoMetadataMock.first(),
                onRouteCardClick = {},
                onDeleteClick = {}
            )
        }
    }
}
