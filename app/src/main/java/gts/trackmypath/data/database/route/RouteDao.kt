package gts.trackmypath.data.database.route

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import gts.trackmypath.data.database.photometadata.PhotoMetadataEntity
import kotlinx.coroutines.flow.Flow

/**
 *
 * A database @Transaction guarantees two main things:
 * 1. Atomicity: It's an "all or nothing" operation.
 *    If you are executing multiple queries, and one fails, all previous changes in that transaction are rolled back.
 * 2.Consistency: It locks the database (or the specific tables involved) momentarily.
 *   This ensures that no other thread can modify the data while your query is running,
 *   giving you a consistent snapshot of the data.
 *
 * • Always use @Transaction for @Relation POJOs.
 * • Always use @Transaction for Multimap returns (Map<K, V>) unless it's a single JOIN query
 * However, it is still considered a best practice to keep @Transaction on queries that return a Map
 * or List of joined data. If the result set is large,
 * it ensures the entire mapping process happens consistently without Cursor Window swapping issues.
 */
@Dao
interface RouteDao {

    @Insert
    suspend fun insertRoute(route: RouteEntity): Long // returns the auto-generated routeId

    @Query("UPDATE routes SET route_display_name = :displayName, metadata = :metadata WHERE routeId = :routeId")
    suspend fun finishRouteById(routeId: Long, displayName: String, metadata: Map<String, String>)

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
    WHERE routes.route_display_name IS NOT NULL  
    ORDER BY routes.route_created_at DESC, photo_metadata.photo_created_at DESC
""")
    fun observeAllRoutesWithPhotos(): Flow<Map<RouteEntity, List<PhotoMetadataEntity>>>
}
