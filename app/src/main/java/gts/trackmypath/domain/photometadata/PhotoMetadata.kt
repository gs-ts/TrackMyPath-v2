package gts.trackmypath.domain.photometadata

data class PhotoMetadata(
    val id: Long,
    val placeId: String,
    val photoUri: String,
    val location: Location,
    val googleMapsUri: String?,
    val generativeSummary: String?,
    val neighborhoodSummary: String?,
) {

    data class Location(
        val latitude: Double,
        val longitude: Double
    )
}
