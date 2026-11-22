package gts.trackmypath.di

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import gts.trackmypath.data.datastore.FilterPreferences
import gts.trackmypath.data.datastore.FilterPreferencesSerializer
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideFilterPreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<FilterPreferences> {
        return DataStoreFactory.create(
            serializer = FilterPreferencesSerializer,
            // If the file can't be read, start again with no filters. Otherwise every read fails:
            // the filter sheet crashes and tracking saves no photos.
            corruptionHandler = ReplaceFileCorruptionHandler { exception ->
                Log.e("DataStoreModule", "Filter preferences unreadable, resetting them", exception)
                FilterPreferencesSerializer.defaultValue
            },
            produceFile = { File(context.filesDir, "datastore/filter_preferences.json") }
        )
    }
}
