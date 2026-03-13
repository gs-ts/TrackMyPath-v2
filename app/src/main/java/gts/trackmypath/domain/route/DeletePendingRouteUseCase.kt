package gts.trackmypath.domain.route

import javax.inject.Inject

class DeletePendingRouteUseCase @Inject constructor(private val routeRepository: RouteRepository) {

    suspend operator fun invoke(routeId: RouteId) {
        routeRepository.deleteRoute(routeId = routeId)
    }
}
