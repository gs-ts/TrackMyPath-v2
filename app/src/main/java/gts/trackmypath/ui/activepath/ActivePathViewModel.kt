package gts.trackmypath.ui.activepath

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import gts.trackmypath.domain.FetchPhotoMetadataForLocationUseCase
import gts.trackmypath.domain.PhotoMetadata
import gts.trackmypath.data.LocationProvider
import gts.trackmypath.ui.activepath.ActivePathViewModel.State.TrackingState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivePathViewModel @Inject constructor(
    private val locationProvider: LocationProvider,
    private val fetchPhotoMetadataForLocationUseCase: FetchPhotoMetadataForLocationUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var locationUpdatesJob: Job? = null

    fun onTrackPathClick() {
        _state.update { state ->
            when (state.trackingState) {
                TrackingState.STOPPED -> {
                    collectLocationUpdates()
                    state.copy(trackingState = TrackingState.TRACKING)
                }

                TrackingState.TRACKING -> {
                    stopLocationUpdates()
                    state.copy(
                        trackingState = TrackingState.STOPPED,
                        photos = persistentListOf()
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun collectLocationUpdates() {
        locationUpdatesJob = locationProvider
            .locationFlow()
            .mapLatest { location ->
                val photo = fetchPhotoMetadataForLocationUseCase(latLng = LatLng(location.latitude, location.longitude))
                Log.d("ActivePathViewModel", "photo received: ${photo?.id}")

                photo?.let {
                    _state.update { state ->
                        state.copy(
                            photos = state.photos.add(photo)
                        )
                    }
                }
            }.launchIn(viewModelScope)
    }

    private fun stopLocationUpdates() {
        Log.d("ActivePathViewModel", "stop location updates")
        locationUpdatesJob?.cancel()
    }

    data class State(
        val trackingState: TrackingState = TrackingState.STOPPED,
        val photos: PersistentList<PhotoMetadata> = persistentListOf()
    ) {

        enum class TrackingState {
            TRACKING,
            STOPPED,
        }
    }
}
