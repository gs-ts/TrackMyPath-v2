package gts.trackmypath.data.database.route

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Insert
    suspend fun insertRoute(route: RouteEntity): Long // returns the auto-generated routeId

    // Updates the route when the user clicks "Stop" and adds a name/metadata
    @Update
    suspend fun updateRoute(route: RouteEntity)

    @Query("SELECT * FROM routes WHERE routeId = :routeId")
    suspend fun getRouteById(routeId: Long): RouteEntity?

    @Query("DELETE FROM routes WHERE routeId = :routeId")
    suspend fun deleteRouteById(routeId: Long)

    @Transaction
    @Query("SELECT * FROM routes WHERE routeId = :routeId")
    fun observeRouteWithPhotosById(routeId: Long): Flow<RouteWithPhotoMetadataEntity?>

    @Transaction
    @Query("SELECT * FROM routes ORDER BY created_at DESC")
    fun observeAllRoutesWithPhotos(): Flow<List<RouteWithPhotoMetadataEntity>>
}
