package gts.trackmypath.data.database.route

import androidx.room.Embedded
import androidx.room.Relation
import gts.trackmypath.data.database.photo.PhotoMetadataEntity

data class RouteWithPhotoMetadata(
    @Embedded val route: RouteEntity,
    @Relation(
        parentColumn = "routeId",
        entityColumn = "route_id"
    )
    val photos: List<PhotoMetadataEntity>
)
