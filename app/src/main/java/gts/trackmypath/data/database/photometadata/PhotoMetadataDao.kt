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

    @Query("DELETE FROM photo_metadata")
    suspend fun deleteAll()

    @Query("SELECT photo_uri FROM photo_metadata WHERE place_id = :placeId LIMIT 1")
    suspend fun getPhotoUriByPlaceId(placeId: String): String?

    @Query("UPDATE photo_metadata SET photo_uri = :newUri WHERE place_id = :placeId")
    suspend fun updatePhotoUri(placeId: String, newUri: String)

    @Query("DELETE FROM photo_metadata WHERE place_id = :placeId")
    suspend fun deleteByPlaceId(placeId: String)
}
