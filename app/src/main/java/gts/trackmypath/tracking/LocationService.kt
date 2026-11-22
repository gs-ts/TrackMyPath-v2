package gts.trackmypath.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import gts.trackmypath.R
import gts.trackmypath.domain.route.RouteId
import gts.trackmypath.domain.tracking.FindPhotosAlongRouteUseCase
import gts.trackmypath.ui.MainActivity
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class LocationService : LifecycleService() {

    @Inject
    lateinit var routeTracker: ServiceRouteTracker

    @Inject
    lateinit var findPhotosAlongRouteUseCase: FindPhotosAlongRouteUseCase

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
            routeTracker.onTrackingStopped()
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
                routeTracker.onTrackingStopped()
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
            findPhotosAlongRoute(routeId = RouteId(id = newRouteId))
        }
        routeTracker.onTrackingStarted(routeId = RouteId(id = routeId))
    }

    private fun findPhotosAlongRoute(routeId: RouteId) {
        findPhotosAlongRouteUseCase(routeId = routeId)
            .onEach { result ->
                result
                    .onSuccess { Log.d("LocationService", "photo received") }
                    .onFailure { Log.e("LocationService", "error fetching photo", it) }
            }
            .catch { exception ->
                // Location updates can't start (e.g. Google Play services is missing or outdated), or the search
                // failed unexpectedly (a bug, or the database). Stop rather than keep a tracking notification that
                // will never produce photos. onDestroy resets the tracking state, so the UI shows that tracking has
                // stopped. We don't retry: the user can tap Play again, which requests location updates again.
                Log.e("LocationService", "Finding photos failed, stopping tracking", exception)
                stopSelf()
            }
            // lifecycleScope is cancelled right after onDestroy, and that stops the location updates.
            .launchIn(scope = lifecycleScope)
    }

    override fun onDestroy() {
        Log.d("LocationService", "onDestroy: Stopping location service")
        routeTracker.onTrackingStopped()
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
