package gts.trackmypath.data.database.photo

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photoMetadataEntity: PhotoMetadataEntity)

    @Query("SELECT * FROM photo_metadata ORDER BY created_at DESC")
    fun observeAllPhotos(): Flow<List<PhotoMetadataEntity>>

    @Query("SELECT * FROM photo_metadata WHERE id = :id")
    suspend fun getById(id: String): PhotoMetadataEntity?

    @Query("DELETE FROM photo_metadata")
    suspend fun deleteAll()
}
