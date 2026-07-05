package gts.trackmypath.data.datastore

import kotlinx.serialization.Serializable

@Serializable
data class FilterPreferences(
    val selectedPlaceFilters: Set<String> = emptySet()
)
