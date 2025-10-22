package gts.trackmypath.domain

import com.google.android.gms.maps.model.LatLng
import javax.inject.Inject

class FetchPhotoMetadataForLocationUseCase @Inject constructor(private val photoRepository: PhotoRepository) {

    suspend operator fun invoke(latLng: LatLng): Result<PhotoMetadata> {
        return photoRepository.fetchPhotoMetadataForLocation(latLng)
    }
}
