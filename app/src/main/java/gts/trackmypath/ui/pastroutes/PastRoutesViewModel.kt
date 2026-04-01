package gts.trackmypath.ui.pastroutes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gts.trackmypath.domain.route.ObserveAllRoutesWithPhotoMetadataUseCase
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.RouteWithPhotoMetadata
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Instant
import kotlin.time.toJavaInstant

@HiltViewModel
class PastRoutesViewModel @Inject constructor(
    observeAllRoutesWithPhotoMetadataUseCase: ObserveAllRoutesWithPhotoMetadataUseCase
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter // TODO maybe inject
        .ofLocalizedDate(FormatStyle.SHORT) // e.g., 1/1/24 in US, 01/01/24 in UK
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())

    val state: StateFlow<List<RouteWithPhotoMetadata>> = observeAllRoutesWithPhotoMetadataUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
            initialValue = emptyList()
        )

    fun onRouteCardClick(routeId: RouteId) {
        Log.d("PastRoutesViewModel", "onRouteCardClick: $routeId")
    }

    fun formatRouteDate(createdAt: Instant): String {
        return "Date: ${dateFormatter.format(createdAt.toJavaInstant())}"
    }
}
