package gts.trackmypath.data

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import gts.trackmypath.di.ApplicationScope
import gts.trackmypath.domain.Coordinates
import gts.trackmypath.domain.LocationUnavailableException
import gts.trackmypath.domain.tracking.LocationSource
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
) : LocationSource {

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
                    trySend(Coordinates(latitude = it.latitude, longitude = it.longitude))
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
            // Close the flow with the exception: collectors of locations get a LocationUnavailableException.
            close(exception)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d("LocationProvider", "Location updates stopped")
        }
    }.shareAsResultsIn(scope = applicationScope)

    // shareIn can't pass an upstream failure on to collectors, so it's shared as a value and thrown again here.
    // Declared after locationUpdates, because property initializers run in order.
    override val locations: Flow<Coordinates> = locationUpdates.map { location ->
        location.getOrElse { exception -> throw LocationUnavailableException(cause = exception) }
    }

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
