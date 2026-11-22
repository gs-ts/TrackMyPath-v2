package gts.trackmypath.domain.photometadata

import gts.trackmypath.domain.Coordinates

data class PhotoMetadata(
    val id: Long,
    val placeId: String,
    val photoUri: String,
    val location: Coordinates, // Where the user was when the photo was found, not where the place is.
    val displayName: String? = null,
    val googleMapsUri: String? = null,
    val generativeSummary: String? = null,
    val neighborhoodSummary: String? = null,
)
