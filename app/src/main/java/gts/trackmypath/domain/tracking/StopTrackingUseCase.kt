package gts.trackmypath.domain.tracking

import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.route.RouteRepository
import javax.inject.Inject
import kotlin.time.Clock

/**
 * Stops tracking and finishes [routeId], the tracked route. The route is finished before the user names it,
 * so it's kept even if the app is closed or killed while the name dialog is open.
 */
class StopTrackingUseCase @Inject constructor(
    private val routeTracker: RouteTracker,
    private val routeRepository: RouteRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(routeId: RouteId) {
        routeTracker.stop()
        // finishRoute runs in the application scope, so leaving the screen right after Stop can't cancel it.
        routeRepository.finishRoute(
            routeId = routeId,
            finishedAt = clock.now(),
            metadata = emptyMap() // TODO
        )
    }
}
