package gts.trackmypath.ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.ui.activepath.shimmer

@Composable
fun PhotoCard(
    modifier: Modifier = Modifier,
    photo: PhotoMetadata
) {
    Card(
        modifier = modifier,
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
