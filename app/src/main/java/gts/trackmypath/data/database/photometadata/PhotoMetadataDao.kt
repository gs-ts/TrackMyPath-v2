package gts.trackmypath.data.database.photometadata

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhotoMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photoMetadataEntity: PhotoMetadataEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM photo_metadata WHERE route_id = :routeId AND place_id = :placeId)")
    suspend fun existsForRoute(routeId: Long, placeId: String): Boolean

//    @Query("SELECT * FROM photo_metadata WHERE id = :id")
//    suspend fun getById(id: String): PhotoMetadataEntity?

    @Query("DELETE FROM photo_metadata")
    suspend fun deleteAll()
}
