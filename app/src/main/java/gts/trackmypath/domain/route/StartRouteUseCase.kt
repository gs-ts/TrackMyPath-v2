package gts.trackmypath.domain.route

import javax.inject.Inject

class StartRouteUseCase @Inject constructor(private val routeRepository: RouteRepository) {

    suspend operator fun invoke(): RouteId {
        return routeRepository.startRoute()
    }
}
