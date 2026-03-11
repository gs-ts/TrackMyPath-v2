package gts.trackmypath.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import gts.trackmypath.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resumeWithException

@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext applicationContext: Context,
    @ApplicationScope applicationScope: CoroutineScope
) {

    init {
        Log.d("LocationProvider", "Init $this")
    }

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices
        .getFusedLocationProviderClient(applicationContext)

    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        TimeUnit.SECONDS.toMillis(INTERVAL_TIME.toLong()),
    )
        .setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(FASTEST_INTERVAL_TIME.toLong()))
        .setMinUpdateDistanceMeters(SMALLEST_DISPLACEMENT_100_METERS)
        .build()

    @SuppressLint("MissingPermission")
    private val locationUpdates = callbackFlow {
        val locationCallback = object : LocationCallback() {

            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    Log.d("LocationProvider", "onLocationResult: ${it.latitude}, ${it.longitude}")
                    trySend(it)
                }
            }
        }

        Log.d("LocationProvider", "Starting location updates")

//        val initialLocation = getLastKnownLocation()
//        initialLocation?.let {
//            Log.d("LocationProvider", "Initial last location: ${it.latitude}, ${it.longitude}")
//            trySend(initialLocation)
//        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper(),
        ).addOnFailureListener { exception ->
            Log.e("LocationProvider", "requestLocationUpdates failed: $exception")
            close(exception) // in case of exception, close the Flow
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d("LocationProvider", "Location updates stopped")
        }
    }.shareIn(
        applicationScope,
        replay = 0,
        started = SharingStarted.WhileSubscribed(),
    )

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? = suspendCancellableCoroutine { continuation ->
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                continuation.resume(value = location) { cause, _, _ ->
                    Log.d("LocationProvider", "lastLocation cancellation")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("LocationProvider", "lastLocation failed: $exception")
                continuation.resumeWithException(exception)
            }
    }

    fun locationFlow(): Flow<Location> = locationUpdates

    companion object {
        private const val SMALLEST_DISPLACEMENT_100_METERS = 100F
        private const val INTERVAL_TIME = 45
        private const val FASTEST_INTERVAL_TIME = 15
    }
}
