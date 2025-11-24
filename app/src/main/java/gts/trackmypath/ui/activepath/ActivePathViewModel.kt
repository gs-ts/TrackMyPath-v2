package gts.trackmypath.ui.activepath

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import gts.trackmypath.domain.FetchPhotoMetadataForLocationUseCase
import gts.trackmypath.domain.PhotoMetadata
import gts.trackmypath.ui.activepath.ActivePathViewModel.State.TrackingState
import gts.trackmypath.ui.service.ServiceStateHolder
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivePathViewModel @Inject constructor(
    private val serviceStateHolder: ServiceStateHolder,
    private val fetchPhotoMetadataForLocationUseCase: FetchPhotoMetadataForLocationUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    val event = state.map { it.event }

    private var locationUpdatesJob: Job? = null

    init {
        observeServiceEvents()
    }

    fun onTrackPathClick() {
        _state.update { state ->
            when (state.trackingState) {
                TrackingState.STOPPED -> {
                    state.copy(
                        trackingState = TrackingState.STARTED,
                        event = State.Event.StartLocationService
                    )
                }

                TrackingState.STARTED -> {
                    state.copy(
                        trackingState = TrackingState.STOPPED,
                        photos = persistentListOf(),
                        event = State.Event.StopLocationService
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeServiceEvents() {
        viewModelScope.launch {
            serviceStateHolder.isServiceRunning
                .collectLatest { isServiceRunning ->
                    if (isServiceRunning) {
                        if (state.value.trackingState == TrackingState.STOPPED) {
                            _state.update { state ->
                                state.copy(trackingState = TrackingState.STARTED)
                            }
                        }
                        collectLocationUpdates()
                    } else {
                        stopLocationUpdates()
                    }
                }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun collectLocationUpdates() {
        locationUpdatesJob = serviceStateHolder
            .locationFlow
            .mapLatest { location ->
                if (location != null) {
                    fetchPhotoMetadataForLocationUseCase(
                        latLng = LatLng(
                            location.latitude,
                            location.longitude
                        )
                    ).onSuccess { photo ->
                        Log.d("ActivePathViewModel", "photo received: ${photo.id}")
                        _state.update { state ->
                            state.copy(
                                photos = state.photos.add(photo)
                            )
                        }
                    }.onFailure {
                        Log.e("ActivePathViewModel", "error fetching photo", it)
                    }
                }
            }.launchIn(scope = viewModelScope)
    }

    private fun stopLocationUpdates() {
        Log.d("ActivePathViewModel", "user clicked stop location updates")
        _state.update { state ->
            state.copy(
                trackingState = TrackingState.STOPPED,
                photos = persistentListOf()
            )
        }
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
    }

    fun eventConsumed() {
        _state.update { state ->
            state.copy(event = null)
        }
    }

    data class State(
        val trackingState: TrackingState = TrackingState.STOPPED,
        val photos: PersistentList<PhotoMetadata> = persistentListOf(),
        val event: Event? = null
    ) {

        sealed interface Event {
            data object StartLocationService : Event
            data object StopLocationService : Event
        }

        enum class TrackingState {
            STARTED,
            STOPPED,
        }
    }
}
