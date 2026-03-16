package gts.trackmypath.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import gts.trackmypath.data.database.AppDatabase
import gts.trackmypath.data.database.photometadata.PhotoMetadataDao
import gts.trackmypath.data.database.route.RouteDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "track-my-path-db"
        )
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    // Clean up unfinished routes (where displayName was never set).
                    db.execSQL("DELETE FROM routes WHERE display_name IS NULL")
                }
            })
            // .fallbackToDestructiveMigration() // Use only during dev if you change schema often
            .build()
    }

    @Provides
    fun providePhotoMetadataDao(database: AppDatabase): PhotoMetadataDao {
        return database.photoMetadataDao()
    }

    @Provides
    fun provideRouteDao(database: AppDatabase): RouteDao {
        return database.routeDao()
    }
}
