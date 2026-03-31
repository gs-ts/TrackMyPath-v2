package gts.trackmypath.ui.mockdata

import gts.trackmypath.domain.photometadata.PhotoMetadata
import kotlinx.collections.immutable.persistentListOf

val photoMetadataMock = persistentListOf(
    PhotoMetadata(
        id = 1L,
        placeId = "p1",
        photoUri = "https://example.com/1.jpg",
        location = PhotoMetadata.Location(latitude = 0.0, longitude = 0.0),
        displayName = "example place 1"
    ),
    PhotoMetadata(
        id = 2L,
        placeId = "p2",
        photoUri = "https://example.com/2.jpg",
        location = PhotoMetadata.Location(latitude = 0.0, longitude = 0.0),
        displayName = "a very very much long example place with a lot of text that should be truncated after two lines"
    ),
    PhotoMetadata(
        id = 3L,
        placeId = "p3",
        photoUri = "https://example.com/3.jpg",
        location = PhotoMetadata.Location(latitude = 0.0, longitude = 0.0)
    )
)
