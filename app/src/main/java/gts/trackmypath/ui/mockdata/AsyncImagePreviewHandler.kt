package gts.trackmypath.ui.mockdata

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler

@OptIn(ExperimentalCoilApi::class)
val previewHandler = AsyncImagePreviewHandler { request ->
    when (request.data) {
        "https://example.com/1.jpg" -> ColorImage(Color.Red.toArgb())
        "https://example.com/2.jpg" -> ColorImage(Color.Green.toArgb())
        "https://example.com/3.jpg" -> ColorImage(Color.Blue.toArgb())
        else -> ColorImage(Color.Gray.toArgb())
    }
}
