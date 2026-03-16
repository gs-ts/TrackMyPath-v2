package gts.trackmypath.domain.route

import gts.trackmypath.domain.photometadata.PhotoMetadata

data class RouteWithPhotoMetadata(
    val routeId: RouteId,
    val displayName: String?,
    val metadata: Map<String, String>,
    val photoMetadata: List<PhotoMetadata>
)
