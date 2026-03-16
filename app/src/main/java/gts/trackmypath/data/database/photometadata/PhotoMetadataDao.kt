package gts.trackmypath.data.database.photometadata

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhotoMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photoMetadataEntity: PhotoMetadataEntity)

//    @Query("SELECT * FROM photo_metadata WHERE id = :id")
//    suspend fun getById(id: String): PhotoMetadataEntity?

    @Query("DELETE FROM photo_metadata")
    suspend fun deleteAll()
}
