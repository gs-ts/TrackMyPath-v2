package gts.trackmypath.domain.photometadata

data class PhotoMetadata(
    val id: Long,
    val placeId: String,
    val photoUri: String,
    val location: Location,
    val googleMapsUri: String? = null,
    val generativeSummary: String? = null,
    val neighborhoodSummary: String? = null,
) {

    data class Location(
        val latitude: Double,
        val longitude: Double
    )
}
