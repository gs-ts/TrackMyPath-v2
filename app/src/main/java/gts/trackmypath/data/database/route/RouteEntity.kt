package gts.trackmypath.data.database.route

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val routeId: Long = 0,
    @ColumnInfo(name = "route_created_at") val createdAt: Long = System.currentTimeMillis(),
    // Null while the route is being tracked. Set on Stop, or by RecoverInterruptedRoutesCallback.
    @ColumnInfo(name = "route_finished_at") val finishedAt: Long? = null,
    // Null when the user didn't name the route.
    @ColumnInfo(name = "route_display_name") val displayName: String?,
    @ColumnInfo(name = "metadata") val metadata: Map<String, String> = emptyMap()
)
