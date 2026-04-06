package gts.trackmypath.ui.model

import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.RouteWithPhotoMetadata
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.time.toJavaInstant

data class RouteWithPhotoMetadataUiState(
    val routeId: RouteId,
    val displayName: String?,
    val createdAt: String,
    val metadata: ImmutableMap<String, String>,
    val photoMetadata: ImmutableList<PhotoMetadata>
)

private val localDateFormatter = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.SHORT)
    .withLocale(Locale.getDefault()) // TODO
    .withZone(ZoneId.systemDefault())

fun List<RouteWithPhotoMetadata>.toUiState(): ImmutableList<RouteWithPhotoMetadataUiState> {

    return map { routeWithPhotoMetadata ->
        routeWithPhotoMetadata.toUiState()
    }.toImmutableList()
}

fun RouteWithPhotoMetadata.toUiState() = RouteWithPhotoMetadataUiState(
    routeId = routeId,
    displayName = displayName,
    createdAt = "Date: ${localDateFormatter.format(createdAt.toJavaInstant())}",
    metadata = metadata.toPersistentMap(),
    photoMetadata = photoMetadata.toImmutableList()
)
