package gts.trackmypath.data.database.route

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import gts.trackmypath.data.database.photometadata.PhotoMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Insert
    suspend fun insertRoute(route: RouteEntity): Long // returns the auto-generated routeId

    @Query("UPDATE routes SET route_finished_at = :finishedAt, metadata = :metadata WHERE routeId = :routeId")
    suspend fun finishRouteById(routeId: Long, finishedAt: Long, metadata: Map<String, String>)

    @Query("SELECT * FROM routes WHERE routeId = :routeId")
    suspend fun getRouteById(routeId: Long): RouteEntity?

    @Query("UPDATE routes SET route_display_name = :newDisplayName WHERE routeId = :routeId")
    suspend fun updateRouteName(routeId: Long, newDisplayName: String)

    @Query("DELETE FROM routes WHERE routeId = :routeId")
    suspend fun deleteRouteById(routeId: Long)

    @Query("""
    SELECT * FROM routes 
    LEFT JOIN photo_metadata ON routes.routeId = photo_metadata.route_id 
    WHERE routes.routeId = :routeId 
    ORDER BY photo_metadata.photo_created_at DESC
""")
    fun observeRouteWithPhotosMap(routeId: Long): Flow<Map<RouteEntity, List<PhotoMetadataEntity>>>

    @Query("""
    SELECT * FROM routes 
    LEFT JOIN photo_metadata ON routes.routeId = photo_metadata.route_id 
    WHERE routes.route_finished_at IS NOT NULL
    ORDER BY routes.route_created_at DESC, photo_metadata.photo_created_at DESC
""")
    fun observeAllRoutesWithPhotos(): Flow<Map<RouteEntity, List<PhotoMetadataEntity>>>
}
