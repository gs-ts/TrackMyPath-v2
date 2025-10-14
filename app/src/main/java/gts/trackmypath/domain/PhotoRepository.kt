package gts.trackmypath.domain

import com.google.android.gms.maps.model.LatLng

interface PhotoRepository {

    suspend fun fetchPhotoMetadataForLocation(latLng: LatLng): PhotoMetadata?
}
