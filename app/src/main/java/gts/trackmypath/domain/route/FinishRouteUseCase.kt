package gts.trackmypath.domain.route

import javax.inject.Inject
import kotlin.time.Clock

class FinishRouteUseCase @Inject constructor(
    private val routeRepository: RouteRepository,
    private val clock: Clock,
) {

    suspend operator fun invoke(routeId: RouteId) {
        routeRepository.finishRoute(
            routeId = routeId,
            finishedAt = clock.now(),
            metadata = emptyMap() // TODO
        )
    }
}
