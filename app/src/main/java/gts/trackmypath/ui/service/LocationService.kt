package gts.trackmypath.ui.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
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
    lateinit var serviceStateHolder: ServiceStateHolder

    @Inject
    lateinit var fetchPhotoMetadataForLocationUseCase: FetchPhotoMetadataForLocationUseCase

    private var locationUpdatesJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("LocationService", "onCreate $this")

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action == ACTION_STOP_SERVICE || arePermissionsGranted().not()) {
            serviceStateHolder.setServiceRunning(isRunning = false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_STICKY
        }

        val routeId = intent?.getLongExtra("EXTRA_ROUTE_ID", -1L)
        if (routeId != null && routeId != -1L) {
            startForegroundLocationService(routeId = routeId)
            serviceStateHolder.setServiceRunning(true)
        } else {
            stopSelf() // kill the service if it was started without a valid routeId
        }

        return START_STICKY
    }

    @Suppress("TooGenericExceptionCaught")
    private fun startForegroundLocationService(routeId: Long) {
        try {
            startForeground(
                NOTIFICATION_ID,
                getServiceNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )

            if (locationUpdatesJob == null) {
                collectLocationUpdates(routeId = routeId)
            }
        } catch (exception: Exception) {
            Log.e("LocationService", "Failed to start foreground service", exception)
            serviceStateHolder.setServiceRunning(false) // Reset state if it crashes
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun collectLocationUpdates(routeId: Long) {
        locationUpdatesJob = locationProvider
            .locationFlow()
            .mapLatest { location ->
                Log.d("LocationService", "New location received: $location")
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
            }
            .launchIn(scope = applicationScope)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d("LocationService", "onDestroy: Stopping location service")
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
        serviceStateHolder.setServiceRunning(isRunning = false)
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
        val stopSelfIntent = Intent(this, LocationService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopSelfPendingIntent = PendingIntent.getService(
            this,
            1,
            stopSelfIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

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
            .addAction(0, "Stop tracking", stopSelfPendingIntent)
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
        private const val NOTIFICATION_CHANNEL_NAME = "track_my_path_notification _channel"
        const val ACTION_STOP_SERVICE = "stop_service_action"
    }
}
