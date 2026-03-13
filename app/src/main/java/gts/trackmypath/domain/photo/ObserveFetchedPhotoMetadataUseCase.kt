package gts.trackmypath.domain.photo

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveFetchedPhotoMetadataUseCase @Inject constructor(private val photoRepository: PhotoRepository) {

    val photos = photoRepository.observePhotos()

    operator fun invoke(): Flow<List<PhotoMetadata>> {
        return photoRepository.observePhotos()
    }
}
