package gts.trackmypath.data.database.route

import androidx.room.Embedded
import androidx.room.Relation
import gts.trackmypath.data.database.photometadata.PhotoMetadataEntity
import gts.trackmypath.data.database.photometadata.toDomain
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.RouteWithPhotoMetadata

data class RouteWithPhotoMetadataEntity(
    @Embedded val route: RouteEntity,
    @Relation(
        parentColumn = "routeId",
        entityColumn = "route_id"
    )
    val photoMetadata: List<PhotoMetadataEntity>
)

fun RouteWithPhotoMetadataEntity.toDomain() = RouteWithPhotoMetadata(
    routeId = RouteId(id = route.routeId),
    displayName = route.displayName,
    metadata = route.metadata,
    photoMetadata = photoMetadata.map { photoMetadataEntity ->
        photoMetadataEntity.toDomain()
    }
)
