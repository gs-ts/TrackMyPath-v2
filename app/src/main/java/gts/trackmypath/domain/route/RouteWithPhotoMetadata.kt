package gts.trackmypath.domain.route

import gts.trackmypath.domain.photometadata.PhotoMetadata
import kotlin.time.Instant

data class RouteWithPhotoMetadata(
    val routeId: RouteId,
    val displayName: String?,
    val createdAt: Instant,
    val metadata: Map<String, String>,
    val photoMetadata: List<PhotoMetadata>
)
