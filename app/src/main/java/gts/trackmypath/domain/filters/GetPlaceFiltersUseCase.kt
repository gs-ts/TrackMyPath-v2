package gts.trackmypath.domain.filters

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPlaceFiltersUseCase @Inject constructor(
    private val filterPreferencesDataStore: FilterPreferencesDataStore
) {
    operator fun invoke(): Flow<Set<PlaceFilter>> {
        return filterPreferencesDataStore.placeFilters
    }
}
