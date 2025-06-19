package gts.trackmypath.ui.activepath

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gts.trackmypath.ui.activepath.ActivePathViewModel.State.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivePathViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun onTrackPathClick() {
        viewModelScope.launch {
            _state.update { state ->
                state.copy(
                    trackingState = when (state.trackingState) {
                        TrackingState.TRACKING -> TrackingState.STOPPED
                        TrackingState.STOPPED -> TrackingState.TRACKING
                    },
                )
            }
        }
    }

    data class State(
        val trackingState: TrackingState = TrackingState.STOPPED,
    ) {

        enum class TrackingState {
            TRACKING,
            STOPPED,
        }
    }
}
