package gts.trackmypath.ui.mockdata

import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.RouteWithPhotoMetadata
import kotlinx.collections.immutable.persistentListOf
import kotlin.time.Instant

@Suppress("MagicNumber")
val routesWithPhotoMetadataMock = persistentListOf(
    RouteWithPhotoMetadata(
        routeId = RouteId(1),
        displayName = "Morning Walk",
        createdAt = Instant.parse("2026-04-01T08:00:00Z"),
        photoMetadata = photoMetadataMock,
        metadata = emptyMap()
    ),
    RouteWithPhotoMetadata(
        routeId = RouteId(2),
        displayName = "evening Walk",
        createdAt = Instant.parse("2026-04-02T18:00:00Z"),
        photoMetadata = photoMetadataMock,
        metadata = emptyMap()
    ),
    RouteWithPhotoMetadata(
        routeId = RouteId(3),
        displayName = "short Walk",
        createdAt = Instant.parse("2026-04-03T12:00:00Z"),
        photoMetadata = photoMetadataMock.removeAt(index = 0),
        metadata = emptyMap()
    )
)
