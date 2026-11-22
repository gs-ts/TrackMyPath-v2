package gts.trackmypath.domain.tracking

import gts.trackmypath.domain.route.RouteRepository
import javax.inject.Inject

/** Creates a route and starts tracking it. */
class StartTrackingUseCase @Inject constructor(
    private val routeRepository: RouteRepository,
    private val routeTracker: RouteTracker,
) {

    suspend operator fun invoke() {
        routeTracker.start(routeId = routeRepository.startRoute())
    }
}
