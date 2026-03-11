package gts.trackmypath.data.database.route

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val routeId: Long = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "display_name") val displayName: String?,
    @ColumnInfo(name = "metadata") val metadata: Map<String, String> = emptyMap()
)
