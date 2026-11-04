package gts.trackmypath.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

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
        .setMinUpdateDistanceMeters(MIN_UPDATE_DISTANCE_METERS)
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

        @Suppress("InjectDispatcher")
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            Dispatchers.Default.asExecutor(), // Keep GPS callbacks completely off the main thread
            locationCallback
        ).addOnFailureListener { exception ->
            Log.e("LocationProvider", "requestLocationUpdates failed: $exception")
            // Close the Flow with the exception: shareAsResultsIn delivers it to collectors as a failure.
            close(exception)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d("LocationProvider", "Location updates stopped")
        }
    }.shareAsResultsIn(scope = applicationScope)

    /**
     * Emits a [Result.success] for each new location. If location updates can't be started
     * (e.g. Google Play services is missing, disabled or outdated), it emits one [Result.failure]
     * and requests location updates again only when a new collector arrives. It never retries on its own.
     */
    fun locationFlow(): Flow<Result<Location>> = locationUpdates

    companion object {
        // Updates closer than this to the last delivered location are dropped (e.g. while standing still),
        // and each dropped update saves a Places API call.
        // This is a filter applied only when the interval fires.
        // The system does not continuously track your distance in the background between intervals.
        private const val MIN_UPDATE_DISTANCE_METERS = 30F

        // This dictates how often the GPS hardware actually "wakes up" to compute your location.
        private const val INTERVAL_TIME = 30

        // This is the throttle limit.
        // If another app (like Google Maps) is forcing the GPS to stay awake,
        // your app is allowed to receive those updates as fast as every 15 seconds, but no faster.
        private const val FASTEST_INTERVAL_TIME = 15
    }
}

/**
 * Shares this flow in [scope], emitting each value as a [Result.success] and an upstream failure
 * as one final [Result.failure].
 *
 * Why not just [shareIn]: an exception thrown upstream of [shareIn] never reaches the collectors.
 * It fails the sharing coroutine in [scope] instead, and [ApplicationScope] has no exception handler,
 * so on Android it would crash the app.
 *
 * Sharing stops when the last collector leaves, so the next collector restarts the upstream.
 */
internal fun <T> Flow<T>.shareAsResultsIn(scope: CoroutineScope): SharedFlow<Result<T>> =
    map { value -> Result.success(value) }
        .catch { exception -> emit(Result.failure(exception)) }
        .shareIn(
            scope = scope,
            replay = 0,
            started = SharingStarted.WhileSubscribed(),
        )
