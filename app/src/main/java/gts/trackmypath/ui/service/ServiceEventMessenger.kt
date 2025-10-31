package gts.trackmypath.ui.service

import android.location.Location
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceEventMessenger @Inject constructor() {

    private val _locationFlow = MutableStateFlow<Location?>(null)
    val locationFlow: StateFlow<Location?> = _locationFlow.asStateFlow()

    fun updateLocation(location: Location) {
        _locationFlow.value = location
    }

    private val _serviceEvents = MutableSharedFlow<ServiceEvent>(replay = 1)
    val serviceEvents: SharedFlow<ServiceEvent> = _serviceEvents.asSharedFlow()

    fun sendEvent(serviceEvent: ServiceEvent) {
        _serviceEvents.tryEmit(value = serviceEvent)
    }

    sealed interface ServiceEvent {
        data object StopTracking : ServiceEvent
    }
}
