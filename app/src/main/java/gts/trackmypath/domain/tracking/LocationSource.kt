package gts.trackmypath.domain.tracking

import gts.trackmypath.domain.Coordinates
import gts.trackmypath.domain.LocationUnavailableException
import kotlinx.coroutines.flow.Flow

/** The user's location while a route is tracked. */
interface LocationSource {

    /**
     * Emits each new location. Fails with [LocationUnavailableException] if location updates can't be started
     * (e.g. Google Play services is missing, disabled or outdated). Collecting again requests location updates
     * again. It never retries on its own.
     */
    val locations: Flow<Coordinates>
}
