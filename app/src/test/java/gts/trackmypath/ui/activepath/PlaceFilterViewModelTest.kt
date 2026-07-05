package gts.trackmypath.ui.activepath

import gts.trackmypath.MainDispatcherRule
import gts.trackmypath.domain.filters.ApplyPlaceFiltersUseCase
import gts.trackmypath.domain.filters.FilterPreferencesDataStore
import gts.trackmypath.domain.filters.GetPlaceFiltersUseCase
import gts.trackmypath.domain.filters.PlaceFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaceFilterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `given no place filters when on initial state then state is correct`() = runTest {
        val viewModel = createViewModel()

        assertEquals(PlaceFilterViewModel.State(), viewModel.state.value)
    }

    @Test
    fun `onPlaceFilterSelect toggles place filter selection`() = runTest {
        val viewModel = createViewModel()
        val filter = PlaceFilter.CULTURE

        viewModel.onPlaceFilterSelect(placeFilter = filter)
        assertTrue(actual = viewModel.state.value.selectedPlaceFilters.contains(filter))

        viewModel.onPlaceFilterSelect(placeFilter = filter)
        assertFalse(actual = viewModel.state.value.selectedPlaceFilters.contains(filter))
    }

    @Test
    fun `given selected place filters when onResetPlaceFiltersClick then clears all selected filters`() =
        runTest {
            val viewModel = createViewModel()
            val filter1 = PlaceFilter.CULTURE
            val filter2 = PlaceFilter.ENTERTAINMENT

            viewModel.onPlaceFilterSelect(filter1)
            viewModel.onPlaceFilterSelect(filter2)
            assertEquals(expected = 2, actual = viewModel.state.value.selectedPlaceFilters.size)

            viewModel.onResetPlaceFiltersClick()
            assertTrue(actual = viewModel.state.value.selectedPlaceFilters.isEmpty())
        }

    @Test
    fun `given saved filters in datastore when viewmodel initialized then state contains saved filters`() =
        runTest {
            val savedFilters = setOf(PlaceFilter.CULTURE, PlaceFilter.SHOPPING)
            val fakeDataStore = FilterPreferencesDataStoreFake(initialFilters = savedFilters)

            val viewModel = PlaceFilterViewModel(
                getPlaceFiltersUseCase = GetPlaceFiltersUseCase(fakeDataStore),
                applyPlaceFiltersUseCase = ApplyPlaceFiltersUseCase(fakeDataStore)
            )

            advanceUntilIdle()

            assertEquals(expected = 2, actual = viewModel.state.value.selectedPlaceFilters.size)
            assertTrue(actual = viewModel.state.value.selectedPlaceFilters.contains(PlaceFilter.CULTURE))
            assertTrue(actual = viewModel.state.value.selectedPlaceFilters.contains(PlaceFilter.SHOPPING))
        }

    @Test
    fun `given selected filters when onClose then filters are saved and bottom sheet dismiss state is true`() = runTest {
        val fakeDataStore = FilterPreferencesDataStoreFake()
        val viewModel = PlaceFilterViewModel(
            getPlaceFiltersUseCase = GetPlaceFiltersUseCase(fakeDataStore),
            applyPlaceFiltersUseCase = ApplyPlaceFiltersUseCase(fakeDataStore)
        )

        viewModel.onPlaceFilterSelect(PlaceFilter.SPORTS)

        viewModel.onClose()
        advanceUntilIdle()

        assertTrue(actual = viewModel.state.value.isBottomSheetDismissed)

        assertEquals(
            expected = setOf(PlaceFilter.SPORTS),
            actual = fakeDataStore.savedFilters // We need to expose this property in the fake
        )
    }

    // --- Helper classes and setup ---

    private fun createViewModel(): PlaceFilterViewModel {
        val placeFilterViewModel = PlaceFilterViewModel(
            getPlaceFiltersUseCase = GetPlaceFiltersUseCase(FilterPreferencesDataStoreFake()),
            applyPlaceFiltersUseCase = ApplyPlaceFiltersUseCase(FilterPreferencesDataStoreFake()),
        )

        return placeFilterViewModel
    }

    class FilterPreferencesDataStoreFake(initialFilters: Set<PlaceFilter> = emptySet()) :
        FilterPreferencesDataStore {

        var savedFilters: Set<PlaceFilter>? = null
            private set

        override val placeFilters: Flow<Set<PlaceFilter>> = flowOf(initialFilters)

        override suspend fun setPlaceFilters(filters: Set<PlaceFilter>) {
            savedFilters = filters
        }
    }
}
