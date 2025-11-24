package gts.trackmypath

import app.cash.turbine.test
import com.google.android.gms.maps.model.LatLng
import gts.trackmypath.domain.FetchPhotoMetadataForLocationUseCase
import gts.trackmypath.domain.PhotoMetadata
import gts.trackmypath.domain.PhotoRepository
import gts.trackmypath.ui.activepath.ActivePathViewModel
import gts.trackmypath.ui.service.ServiceStateHolder
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals

class ActivePathViewModelTest {

    @Test
    fun `initial state`() = runTest {
        val viewModel = ActivePathViewModel(
            serviceStateHolder = ServiceStateHolder(),
            fetchPhotoMetadataForLocationUseCase = FetchPhotoMetadataForLocationUseCase(
                PhotoRepositoryFake()
            )
        )

        val state = viewModel.state.value

        assertEquals(
            expected = ActivePathViewModel.State.TrackingState.STOPPED,
            actual = state.trackingState
        )

        assertEquals(
            expected = persistentListOf(),
            actual = state.photos
        )
    }

    @Test
    fun `given service is running when viewModel is created then tracking state is started`() =
        runTest {
            val serviceStateHolder = ServiceStateHolder()
            serviceStateHolder.setServiceRunning(isRunning = true)

            val viewModel = ActivePathViewModel(
                serviceStateHolder = serviceStateHolder,
                fetchPhotoMetadataForLocationUseCase = FetchPhotoMetadataForLocationUseCase(
                    PhotoRepositoryFake()
                )
            )

            viewModel.state.test {
                // Consume and discard the initial state if it's STOPPED
                val firstEmission = awaitItem()
                val finalState =
                    if (firstEmission.trackingState == ActivePathViewModel.State.TrackingState.STOPPED) {
                        // If the initial emission was STOPPED, then the next one should be STARTED
                        awaitItem()
                    } else {
                        // Otherwise, the first emission was already STARTED
                        firstEmission
                    }

                assertEquals(
                    expected = ActivePathViewModel.State.TrackingState.STARTED,
                    actual = finalState.trackingState
                )

                assertEquals(
                    expected = persistentListOf(),
                    actual = finalState.photos
                )
            }
        }

}

class PhotoRepositoryFake : PhotoRepository {

    override suspend fun fetchPhotoMetadataForLocation(latLng: LatLng): Result<PhotoMetadata> {
        return Result.success(
            value = PhotoMetadata(
                id = "id",
                photoUri = URI("some-uri")
            )
        )
    }
}
