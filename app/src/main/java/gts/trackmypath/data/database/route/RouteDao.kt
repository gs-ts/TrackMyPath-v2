package gts.trackmypath.data.database.route

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import gts.trackmypath.data.database.photometadata.PhotoMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Insert
    suspend fun insertRoute(route: RouteEntity): Long // returns the auto-generated routeId

    // A route is finished once: finishing it again, e.g. after a second tap on Stop, changes nothing.
    @Query("""
    UPDATE routes SET route_finished_at = :finishedAt, metadata = :metadata
    WHERE routeId = :routeId AND route_finished_at IS NULL
""")
    suspend fun finishRouteById(routeId: Long, finishedAt: Long, metadata: Map<String, String>)

    @Query("SELECT * FROM routes WHERE routeId = :routeId")
    suspend fun getRouteById(routeId: Long): RouteEntity?

    @Query("UPDATE routes SET route_display_name = :newDisplayName WHERE routeId = :routeId")
    suspend fun updateRouteName(routeId: Long, newDisplayName: String)

    @Query("DELETE FROM routes WHERE routeId = :routeId")
    suspend fun deleteRouteById(routeId: Long)

    // Why the queries that join routes and photos use @Transaction:
    // Android reads a result bigger than 2 MB in parts, and runs the query again for each part. Without a
    // transaction, the data can change between two parts. With one, every part sees the same data.
    //
    // Example: Past Routes has 3,000 photos, so Android reads them in two parts. While it reads the first
    // part, the app deletes the photos of a place Google no longer knows. Android counted the rows before
    // that, so it later asks for a row that's gone, and the read crashes with "Couldn't read row X, col Y
    // from CursorWindow". If a route with photos in both parts is renamed at that moment, the list shows it twice.
    @Transaction
    @Query("""
    SELECT * FROM routes 
    LEFT JOIN photo_metadata ON routes.routeId = photo_metadata.route_id 
    WHERE routes.routeId = :routeId 
    ORDER BY photo_metadata.photo_created_at DESC
""")
    fun observeRouteWithPhotosMap(routeId: Long): Flow<Map<RouteEntity, List<PhotoMetadataEntity>>>

    @Transaction
    @Query("""
    SELECT * FROM routes 
    LEFT JOIN photo_metadata ON routes.routeId = photo_metadata.route_id 
    WHERE routes.route_finished_at IS NOT NULL
    ORDER BY routes.route_created_at DESC, photo_metadata.photo_created_at DESC
""")
    fun observeAllRoutesWithPhotos(): Flow<Map<RouteEntity, List<PhotoMetadataEntity>>>
}
