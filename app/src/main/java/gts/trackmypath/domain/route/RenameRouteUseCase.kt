package gts.trackmypath.domain.route

import javax.inject.Inject

class RenameRouteUseCase @Inject constructor(private val routeRepository: RouteRepository) {

    suspend operator fun invoke(routeId: RouteId, name: String) {
        routeRepository.renameRoute(routeId = routeId, name = name)
    }
}
