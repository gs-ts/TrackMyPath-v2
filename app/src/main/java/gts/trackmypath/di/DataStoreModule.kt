package gts.trackmypath.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
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
            produceFile = { File(context.filesDir, "datastore/filter_preferences.json") }
        )
    }
}
