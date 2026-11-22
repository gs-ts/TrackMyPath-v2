package gts.trackmypath.ui.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.PermissionChecker
import dagger.hilt.android.AndroidEntryPoint
import gts.trackmypath.R
import gts.trackmypath.data.LocationProvider
import gts.trackmypath.di.ApplicationScope
import gts.trackmypath.domain.photometadata.FetchPhotoMetadataForLocationUseCase
import gts.trackmypath.domain.photometadata.PhotoMetadata
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.ui.MainActivity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : Service() {

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var locationProvider: LocationProvider

    @Inject
    lateinit var locationServiceStateHolder: LocationServiceStateHolder

    @Inject
    lateinit var fetchPhotoMetadataForLocationUseCase: FetchPhotoMetadataForLocationUseCase

    private var locationUpdatesJob: Job? = null

    // The route this service tracks, set by the first start command.
    private var trackedRouteId: Long? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("LocationService", "onCreate $this")

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (arePermissionsGranted().not()) {
            locationServiceStateHolder.setServiceRunning(isRunning = false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val routeId = intent?.getLongExtra(EXTRA_ROUTE_ID, -1L) ?: -1L
        when {
            routeId == -1L -> stopSelf() // kill the service if it was started without a valid routeId
            startForegroundLocationService() -> trackRoute(requestedRouteId = routeId)
            else -> {
                // Android didn't let the service run in the foreground, so nothing is tracked. Say so, and stop.
                locationServiceStateHolder.setServiceRunning(isRunning = false)
                stopSelf()
            }
        }

        return START_STICKY
    }

    // Returns false if Android refused to make this a foreground service.
    @Suppress("TooGenericExceptionCaught")
    private fun startForegroundLocationService(): Boolean {
        return try {
            startForeground(
                NOTIFICATION_ID,
                getServiceNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
            true
        } catch (exception: Exception) {
            Log.e("LocationService", "Failed to start foreground service", exception)
            false
        }
    }

    // One route per tracking session. A second start command, for example from a quick double tap on Start,
    // keeps tracking the first route, so the photos and the screen stay on the same route.
    private fun trackRoute(requestedRouteId: Long) {
        val routeId = trackedRouteId ?: requestedRouteId.also { newRouteId ->
            trackedRouteId = newRouteId
            collectLocationUpdates(routeId = newRouteId)
        }
        locationServiceStateHolder.setServiceRunning(isRunning = true, activeRouteId = RouteId(routeId))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun collectLocationUpdates(routeId: Long) {
        locationUpdatesJob = locationProvider
            .locationFlow()
            .mapLatest { locationResult ->
                locationResult
                    .onSuccess { location ->
                        fetchPhotoMetadataForLocation(routeId = routeId, location = location)
                    }
                    .onFailure { exception ->
                        // Location updates can't start (e.g. Google Play services is missing or outdated).
                        // Stop rather than keep a tracking notification that will never produce photos.
                        // onDestroy resets the tracking state, so the UI shows that tracking has stopped.
                        // We don't retry: the user can tap Play again, which requests location updates again.
                        Log.e("LocationService", "Location updates unavailable, stopping tracking", exception)
                        stopSelf()
                    }
            }
            .launchIn(scope = applicationScope)
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchPhotoMetadataForLocation(routeId: Long, location: Location) {
        Log.d("LocationService", "New location received: $location")
        try {
            fetchPhotoMetadataForLocationUseCase(
                routeId = RouteId(id = routeId),
                location = PhotoMetadata.Location(
                    latitude = location.latitude,
                    longitude = location.longitude
                )
            ).onSuccess {
                Log.d("LocationService", "photo received")
            }.onFailure {
                Log.e("LocationService", "error fetching photo", it)
            }
        } catch (cancellationException: CancellationException) {
            throw cancellationException // mapLatest cancels this fetch when a newer location arrives
        } catch (exception: Exception) {
            // Last-resort guard: this runs in applicationScope, which has no exception handler, so an
            // unexpected failure (e.g. a full disk or unreadable filter preferences) would crash the app.
            // Skip this location and keep tracking: the next location update tries again.
            Log.e("LocationService", "unexpected error fetching photo, skipping this location", exception)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d("LocationService", "onDestroy: Stopping location service")
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
        locationServiceStateHolder.setServiceRunning(isRunning = false)
        super.onDestroy()
    }

    private fun arePermissionsGranted(): Boolean {
        val fineLocationPermission = PermissionChecker.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        return fineLocationPermission == PermissionChecker.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            importance
        )
        val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun getServiceNotification(): Notification {
        // Intent to open the app when notification is clicked, this will launch MainActivity even if app is killed
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            // These flags ensure the activity is brought to front or created if killed
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val activityPendingIntent = PendingIntent.getActivity(
            this,
            2,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("You are tracking your path")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(activityPendingIntent)
            .setColorized(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)

        return builder.build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "track_my_path_notification_id"
        private const val NOTIFICATION_CHANNEL_NAME = "track_my_path_notification_channel"
        const val EXTRA_ROUTE_ID = "extra_route_id"
    }
}
