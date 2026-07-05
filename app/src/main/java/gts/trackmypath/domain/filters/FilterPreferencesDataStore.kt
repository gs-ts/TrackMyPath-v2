package gts.trackmypath.domain.filters

import kotlinx.coroutines.flow.Flow

interface FilterPreferencesDataStore {

    val placeFilters: Flow<Set<PlaceFilter>>

    suspend fun setPlaceFilters(filters: Set<PlaceFilter>)
}
