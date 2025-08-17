package gts.trackmypath.domain

import com.google.android.gms.maps.model.LatLng

interface PhotoRepository {

    suspend fun fetchPhotoForLocation(latLng: LatLng): Photo?
}
