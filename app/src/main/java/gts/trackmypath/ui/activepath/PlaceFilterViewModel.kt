package gts.trackmypath.ui.activepath

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gts.trackmypath.domain.filters.ApplyPlaceFiltersUseCase
import gts.trackmypath.domain.filters.GetPlaceFiltersUseCase
import gts.trackmypath.domain.filters.PlaceFilter
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaceFilterViewModel @Inject constructor(
    private val getPlaceFiltersUseCase: GetPlaceFiltersUseCase,
    private val applyPlaceFiltersUseCase: ApplyPlaceFiltersUseCase
) : ViewModel() {

    val state: StateFlow<State>
        field = MutableStateFlow(State())

    init {
        viewModelScope.launch {
            val currentFilters = getPlaceFiltersUseCase().first()
            state.update { it.copy(selectedPlaceFilters = currentFilters.toPersistentSet()) }
        }
    }

    fun onPlaceFilterSelect(placeFilter: PlaceFilter) {
        state.update { state ->
            val newPlaceFilters = if (state.selectedPlaceFilters.contains(placeFilter)) {
                state.selectedPlaceFilters.removing(element = placeFilter)
            } else {
                state.selectedPlaceFilters.adding(element = placeFilter)
            }
            state.copy(selectedPlaceFilters = newPlaceFilters)
        }
    }

    fun onResetPlaceFiltersClick() {
        state.update { state -> state.copy(selectedPlaceFilters = persistentSetOf()) }
    }

    fun onClose() {
        viewModelScope.launch {
            applyPlaceFiltersUseCase(selectedPlaceFilters = state.value.selectedPlaceFilters)
            state.update { state -> state.copy(isBottomSheetDismissed = true) }
        }
    }

    data class State(
        val selectedPlaceFilters: PersistentSet<PlaceFilter> = persistentSetOf(),
        val isBottomSheetDismissed: Boolean = false
    )
}
