package gts.trackmypath.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import gts.trackmypath.data.database.converter.MetadataConverter
import gts.trackmypath.data.database.photometadata.PhotoMetadataDao
import gts.trackmypath.data.database.photometadata.PhotoMetadataEntity
import gts.trackmypath.data.database.route.RouteDao
import gts.trackmypath.data.database.route.RouteEntity

@Database(
    entities = [PhotoMetadataEntity::class, RouteEntity::class],
    version = 1
)
@TypeConverters(MetadataConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun photoMetadataDao(): PhotoMetadataDao

    abstract fun routeDao(): RouteDao
}
