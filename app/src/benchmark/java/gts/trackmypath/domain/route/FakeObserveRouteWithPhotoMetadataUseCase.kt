package gts.trackmypath.domain.route

import android.util.Log
import gts.trackmypath.domain.photometadata.PhotoMetadata
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject

class FakeObserveRouteWithPhotoMetadataUseCase @Inject constructor() : ObserveRouteWithPhotoMetadataContract {

    override operator fun invoke(routeId: RouteId): Flow<RouteWithPhotoMetadata> = flow {
        // use unique photoUri so Coil doesn't serve everything from cache
        val initialList = MutableList(500) { index ->
            PhotoMetadata(
                id = index.toLong(),
                placeId = "fake_place_$index",
                photoUri = "https://picsum.photos/seed/$index/400/200",
                location = PhotoMetadata.Location(0.0, 0.0),
                generativeSummary = "Fake Image $index",
                neighborhoodSummary = "Fake Neighborhood $index"
            )
        }

        var currentRoute = RouteWithPhotoMetadata(
            routeId = routeId,
            displayName = "Fake Route",
            photoMetadata = initialList,
            metadata = emptyMap()
        )
        emit(currentRoute)

        // give UIAutomator enough time to scroll away from top before new photo arrives
        delay(40_000)

        var nextId = 500L
        while (currentCoroutineContext().isActive) {
            val newPhoto = PhotoMetadata(
                id = nextId,
                placeId = "fake_place_$nextId",
                photoUri = "https://picsum.photos/seed/$nextId/400/200",
                location = PhotoMetadata.Location(0.0, 0.0),
                generativeSummary = "Fake Image $nextId",
                neighborhoodSummary = "Fake Neighborhood $nextId"
            )
            currentRoute = currentRoute.copy(
                photoMetadata = (listOf(newPhoto) + currentRoute.photoMetadata)
            )
            emit(currentRoute)
            nextId++
            delay(5_000) // new photo every 5s while benchmark is running
        }
    }
}
