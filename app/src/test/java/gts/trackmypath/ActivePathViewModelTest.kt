package gts.trackmypath

/*
class ActivePathViewModelTest {

    @Test
    fun `initial state`() = runTest {
        val viewModel = ActivePathViewModel(
            serviceStateHolder = ServiceStateHolder(),
            observeFetchedPhotoMetadataUseCase = ObserveRouteWithPhotoMetadataUseCase(
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
                observeFetchedPhotoMetadataUseCase = ObserveRouteWithPhotoMetadataUseCase(
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

class PhotoRepositoryFake : PhotoMetadataRepository {

    override suspend fun fetchPhotoMetadataForLocation(
        routeId: RouteId,
        location: PhotoMetadata.Location
    ): Result<Unit> {
        return Result.success(Unit)
    }

    override fun observePhotos(): Flow<List<PhotoMetadata>> {
        return flowOf(emptyList())
    }
}
*/