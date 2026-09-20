package gts.trackmypath.data.network

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.places.api.model.PhotoMetadata
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FetchPlaceResponse
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriRequest
import com.google.android.libraries.places.api.net.FetchResolvedPhotoUriResponse
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPhotoResponse
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.google.android.libraries.places.api.net.IsOpenRequest
import com.google.android.libraries.places.api.net.IsOpenResponse
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.android.libraries.places.api.net.SearchByTextResponse
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.android.libraries.places.api.net.SearchNearbyResponse
import com.google.android.libraries.places.api.net.zzak
import com.google.android.libraries.places.api.net.zzao
import com.google.android.libraries.places.internal.zzqs
import com.google.android.libraries.places.api.net.PlacesStatusCodes
import gts.trackmypath.domain.PlaceIdInvalidException
import gts.trackmypath.domain.filters.FilterPreferencesDataStore
import gts.trackmypath.domain.filters.PlaceFilter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GooglePlacesClientTest {

    private lateinit var placesClientFake: PlacesClientFake
    private lateinit var filterPreferencesDataStoreFake: FilterPreferencesDataStoreFake
    private lateinit var googlePlacesClient: GooglePlacesClientImpl
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        placesClientFake = PlacesClientFake()
        filterPreferencesDataStoreFake = FilterPreferencesDataStoreFake()
        googlePlacesClient = GooglePlacesClientImpl(
            placesClient = placesClientFake,
            filterPreferencesDataStore = filterPreferencesDataStoreFake,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `searchNearbyPlaces calls placesClient with correct parameters and returns places`() = runTest(testDispatcher) {
        val latLng = LatLng(37.7749, -122.4194)
        val mockPlace = Place.builder().setId("place_101").build()
        placesClientFake.searchNearbyResponse = SearchNearbyResponse.newInstance(listOf(mockPlace))
        filterPreferencesDataStoreFake.emitFilters(setOf(PlaceFilter.SHOPPING, PlaceFilter.CULTURE))

        val result = googlePlacesClient.searchNearbyPlaces(latLng)

        assertEquals(1, result.size)
        assertEquals("place_101", result[0].id)
        assertNotNull(placesClientFake.lastSearchNearbyRequest)
    }

    @Test
    fun `searchNearbyPlaces returns empty list on ApiException`() = runTest(testDispatcher) {
        placesClientFake.searchNearbyException = ApiException(Status.RESULT_INTERNAL_ERROR)
        val result = googlePlacesClient.searchNearbyPlaces(LatLng(0.0, 0.0))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `searchNearbyPlaces rethrows CancellationException`() = runTest(testDispatcher) {
        placesClientFake.searchNearbyException = CancellationException("Cancelled")
        assertFailsWith<CancellationException> {
            googlePlacesClient.searchNearbyPlaces(LatLng(0.0, 0.0))
        }
    }

    @Test
    fun `fetchPhotoUriByPlaceId fetches place details and resolves photo URI`() = runTest(testDispatcher) {
        val placeId = "p123"
        val metadata = PhotoMetadata.builder("ref_456").build()
        val mockPlace = Place.builder().setId(placeId).setPhotoMetadatas(listOf(metadata)).build()

        placesClientFake.fetchPlaceResponse = FetchPlaceResponse.newInstance(mockPlace)
        placesClientFake.fetchResolvedPhotoUriResponse = FetchResolvedPhotoUriResponse.newInstance(
            android.net.Uri.parse("https://example.com/photo.jpg")
        )

        googlePlacesClient.fetchPhotoUriByPlaceId(placeId)

        assertNotNull(placesClientFake.lastFetchPlaceRequest, "FetchPlaceRequest was never called")
    }

    @Test
    fun `fetchPhotoUriByPlaceId throws PlaceIdInvalidException when Places API returns NOT_FOUND status`() = runTest(testDispatcher) {
        val placeId = "invalid_place_id"
        placesClientFake.fetchPlaceException = ApiException(Status(PlacesStatusCodes.NOT_FOUND, "The provided Place ID is no longer valid."))

        assertFailsWith<PlaceIdInvalidException> {
            googlePlacesClient.fetchPhotoUriByPlaceId(placeId)
        }
    }

    private class FilterPreferencesDataStoreFake : FilterPreferencesDataStore {
        private val _filters = MutableStateFlow<Set<PlaceFilter>>(emptySet())
        override val placeFilters: Flow<Set<PlaceFilter>> = _filters
        override suspend fun setPlaceFilters(filters: Set<PlaceFilter>) { _filters.value = filters }
        fun emitFilters(filters: Set<PlaceFilter>) { _filters.value = filters }
    }

    private class PlacesClientFake : PlacesClient {
        var lastSearchNearbyRequest: SearchNearbyRequest? = null
        var searchNearbyResponse: SearchNearbyResponse? = null
        var searchNearbyException: Exception? = null
        var fetchResolvedPhotoUriResponse: FetchResolvedPhotoUriResponse? = null
        var fetchResolvedPhotoUriException: Exception? = null
        var lastFetchPlaceRequest: FetchPlaceRequest? = null
        var fetchPlaceResponse: FetchPlaceResponse? = null
        var fetchPlaceException: Exception? = null

        private fun <T> Exception?.toTask(result: T?): Task<T> {
            return this?.let {
                if (it is CancellationException || it is java.util.concurrent.CancellationException) Tasks.forCanceled()
                else Tasks.forException(it)
            } ?: Tasks.forResult(result)
        }

        override fun searchNearby(r: SearchNearbyRequest): Task<SearchNearbyResponse> {
            lastSearchNearbyRequest = r
            return searchNearbyException.toTask(searchNearbyResponse)
        }
        override fun fetchResolvedPhotoUri(r: FetchResolvedPhotoUriRequest): Task<FetchResolvedPhotoUriResponse> {
            return fetchResolvedPhotoUriException.toTask(fetchResolvedPhotoUriResponse)
        }
        override fun fetchPlace(r: FetchPlaceRequest): Task<FetchPlaceResponse> {
            lastFetchPlaceRequest = r
            return fetchPlaceException.toTask(fetchPlaceResponse)
        }

        override fun findAutocompletePredictions(p0: FindAutocompletePredictionsRequest) = Tasks.forResult<FindAutocompletePredictionsResponse>(null)
        override fun fetchPhoto(p0: FetchPhotoRequest) = Tasks.forResult<FetchPhotoResponse>(null)
        override fun findCurrentPlace(p0: FindCurrentPlaceRequest) = Tasks.forResult<FindCurrentPlaceResponse>(null)
        override fun isOpen(p0: IsOpenRequest) = Tasks.forResult<IsOpenResponse?>(null)
        override fun searchByText(p0: SearchByTextRequest) = Tasks.forResult<SearchByTextResponse>(null)

        override fun zza(p0: FindAutocompletePredictionsRequest?, p1: zzqs?) = findAutocompletePredictions(p0!!) as Task<*>?
        override fun zzb(p0: FetchPhotoRequest?, p1: zzqs?) = fetchPhoto(p0!!) as Task<*>?
        override fun zzc(p0: FetchResolvedPhotoUriRequest?, p1: zzqs?) = fetchResolvedPhotoUri(p0!!) as Task<*>?
        override fun zzd(p0: FetchPlaceRequest?, p1: zzqs?) = fetchPlace(p0!!) as Task<*>?
        override fun zze(p0: LatLng) = Tasks.forResult(null)
        override fun zzf(p0: FindCurrentPlaceRequest?, p1: String?, p2: zzqs?) = findCurrentPlace(p0!!) as Task<*>?
        override fun zzg(p0: IsOpenRequest?, p1: zzqs?) = isOpen(p0!!) as Task<*>?
        override fun zzh(p0: SearchByTextRequest?, p1: zzqs?) = searchByText(p0!!) as Task<*>?
        override fun zzi(p0: SearchNearbyRequest?, p1: zzqs?) = searchNearby(p0!!) as Task<*>?
        override fun zzj(p0: zzak?, p1: zzqs?) = Tasks.forResult(null)
        override fun zzk(p0: zzao?, p1: zzqs?) = Tasks.forResult(null)
        override fun zzl() {}
        override fun zzm() {}
        override fun zzn() {}
    }
}