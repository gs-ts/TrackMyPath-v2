package gts.trackmypath.domain.filters

import kotlinx.collections.immutable.PersistentSet
import javax.inject.Inject

class ApplyPlaceFiltersUseCase @Inject constructor(
    private val filerPreferencesDataStore: FilterPreferencesDataStore
) {

    suspend operator fun invoke(selectedPlaceFilters: PersistentSet<PlaceFilter>) {
        filerPreferencesDataStore.setPlaceFilters(filters = selectedPlaceFilters)
    }
}
