package gts.trackmypath.data.database.photometadata

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import gts.trackmypath.data.database.route.RouteEntity
import gts.trackmypath.domain.photometadata.PhotoMetadata

@Entity(
    tableName = "photo_metadata",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["routeId"],
            childColumns = ["route_id"],
            onDelete = ForeignKey.CASCADE // if a route is deleted, automatically delete any photos attached to it.
        )
    ],
    indices = [Index("route_id")] // prevents a Room build warning and speeds up queries
)
data class PhotoMetadataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "route_id") val routeId: Long,
    @ColumnInfo(name = "place_id") val placeId: String,
    @ColumnInfo(name = "photo_created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "photo_display_name") val displayName: String?,
    @Embedded(prefix = "location_") val location: Location,
    @ColumnInfo(name = "photo_uri") val photoUri: String,
    @ColumnInfo(name = "google_maps_uri") val googleMapsUri: String?,
    @ColumnInfo(name = "generative_summary") val generativeSummary: String?,
    @ColumnInfo(name = "neighborhood_summary") val neighborhoodSummary: String?,
) {

    data class Location(
        val latitude: Double,
        val longitude: Double
    )
}

fun PhotoMetadataEntity.toDomain() = PhotoMetadata(
    id = id,
    placeId = placeId,
    photoUri = photoUri,
    location = PhotoMetadata.Location(
        latitude = location.latitude,
        longitude = location.longitude
    ),
    googleMapsUri = googleMapsUri,
    generativeSummary = generativeSummary,
    neighborhoodSummary = neighborhoodSummary,
)
