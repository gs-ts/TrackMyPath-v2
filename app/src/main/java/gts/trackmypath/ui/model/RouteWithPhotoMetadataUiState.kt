package gts.trackmypath.ui.model

import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.RouteWithPhotoMetadata
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import kotlin.time.Instant

data class RouteWithPhotoMetadataUiState(
    val routeId: RouteId,
    val displayName: String?,
    val createdAt: Instant,
    val metadata: ImmutableMap<String, String>,
    val photoMetadata: ImmutableList<PhotoMetadata>
)

fun List<RouteWithPhotoMetadata>.toUiState(): ImmutableList<RouteWithPhotoMetadataUiState> {

    return map { routeWithPhotoMetadata ->
        routeWithPhotoMetadata.toUiState()
    }.toImmutableList()
}

fun RouteWithPhotoMetadata.toUiState() = RouteWithPhotoMetadataUiState(
    routeId = routeId,
    displayName = displayName,
    createdAt = createdAt,
    metadata = metadata.toPersistentMap(),
    photoMetadata = photoMetadata.toImmutableList()
)
