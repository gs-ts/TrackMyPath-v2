package gts.trackmypath.domain

import com.google.android.gms.maps.model.LatLng
import javax.inject.Inject

class FetchPhotoForLocationUseCase @Inject constructor(private val photoRepository: PhotoRepository) {

    suspend operator fun invoke(latLng: LatLng): Photo? {
        return photoRepository.fetchPhotoForLocation(latLng)
    }
}
