package gts.trackmypath.data.datastore

import android.util.Log
import androidx.datastore.core.DataStore
import gts.trackmypath.domain.filters.FilterPreferencesDataStore
import gts.trackmypath.domain.filters.PlaceFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FilterPreferencesDataStorImpl @Inject constructor(
    private val dataStore: DataStore<FilterPreferences>
) : FilterPreferencesDataStore {

    override val placeFilters: Flow<Set<PlaceFilter>> = dataStore.data
        .map { preferences ->
            preferences.selectedPlaceFilters.mapNotNull { name ->
                try {
                    PlaceFilter.valueOf(name)
                } catch (illegalArgumentException: IllegalArgumentException) {
                    Log.e("FilterPreferencesDataStore", "${illegalArgumentException.message}")
                    null
                }
            }.toSet()
        }

    override suspend fun setPlaceFilters(filters: Set<PlaceFilter>) {
        dataStore.updateData { currentPreferences ->
            currentPreferences.copy(
                selectedPlaceFilters = filters.map { it.name }.toSet()
            )
        }
    }
}
