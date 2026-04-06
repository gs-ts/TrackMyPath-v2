package gts.trackmypath.ui.pastroutes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import gts.trackmypath.R
import gts.trackmypath.ui.composables.LoadingView
import gts.trackmypath.ui.composables.PhotoCard
import gts.trackmypath.ui.model.RouteWithPhotoMetadataUiState

@Composable
fun PastRouteDetailScreen(
    viewModel: PastRouteDetailViewModel,
    onBackClick: () -> Unit
) {

    val state by viewModel.state.collectAsStateWithLifecycle()

    state.routeWithPhotoMetadata?.let {
        PastRouteDetailContent(
            routesWithPhotoMetadata = it,
            onBackClick = onBackClick
        )
    } ?: LoadingView()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastRouteDetailContent(
    modifier: Modifier = Modifier,
    routesWithPhotoMetadata: RouteWithPhotoMetadataUiState,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = routesWithPhotoMetadata.displayName.orEmpty())
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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp),
        ) {
            items(
                items = routesWithPhotoMetadata.photoMetadata,
                key = { photoMetadata -> photoMetadata.id }
            ) { photoMetadata ->
                PhotoCard(
                    photo = photoMetadata
                )
            }
        }
    }
}
