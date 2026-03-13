package gts.trackmypath.domain.route

import javax.inject.Inject

class FinishRouteUseCase @Inject constructor(private val routeRepository: RouteRepository) {

    suspend operator fun invoke(
        routeId: RouteId,
        displayName: String
    ) {
        routeRepository.finishRoute(
            routeId = routeId,
            displayName = displayName,
            metadata = emptyMap() // TODO
        )
    }
}
