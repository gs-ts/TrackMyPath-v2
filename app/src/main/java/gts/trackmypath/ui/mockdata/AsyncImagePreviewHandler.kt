package gts.trackmypath.ui.mockdata

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImagePreviewHandler
import gts.trackmypath.domain.photometadata.PhotoMetadata

@OptIn(ExperimentalCoilApi::class)
val previewHandler = AsyncImagePreviewHandler { request ->
    val uri = when (val data = request.data) {
        is PhotoMetadata -> data.photoUri
        is String -> data
        else -> null
    }

    when (uri) {
        "https://example.com/1.jpg" -> ColorImage(Color.Red.toArgb())
        "https://example.com/2.jpg" -> ColorImage(Color.Green.toArgb())
        "https://example.com/3.jpg" -> ColorImage(Color.Blue.toArgb())
        else -> ColorImage(Color.Gray.toArgb())
    }
}

@OptIn(ExperimentalCoilApi::class)
val loadingPreviewHandler = AsyncImagePreviewHandler { _, _ ->
    // Returning the Loading state with no painter (null)
    // forces the AsyncImage into the 'onLoading' phase.
    AsyncImagePainter.State.Loading(painter = null)
}
