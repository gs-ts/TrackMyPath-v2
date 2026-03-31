package gts.trackmypath.ui.mockdata

import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.RouteWithPhotoMetadata
import kotlinx.collections.immutable.persistentListOf

val routesWithPhotoMetadataMock = persistentListOf(
    RouteWithPhotoMetadata(
        routeId = RouteId(1),
        displayName = "Morning Walk",
        photoMetadata = photoMetadataMock,
        metadata = emptyMap()
    ),
    RouteWithPhotoMetadata(
        routeId = RouteId(2),
        displayName = "Morning Walk",
        photoMetadata = photoMetadataMock,
        metadata = emptyMap()
    )
)
