package gts.trackmypath.ui.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.PermissionChecker
import dagger.hilt.android.AndroidEntryPoint
import gts.trackmypath.R
import gts.trackmypath.data.LocationProvider
import gts.trackmypath.di.ApplicationScope
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
    lateinit var serviceEventMessenger: ServiceEventMessenger

    private var locationUpdatesJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("LocationService", "onCreate $this")

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            serviceEventMessenger.sendEvent(serviceEvent = ServiceEventMessenger.ServiceEvent.StopTracking)
            stopSelf()
            return START_NOT_STICKY
        }

        checkPermissions()
        startForegroundLocationService()

        return START_NOT_STICKY // Tells the system not to recreate the service after it's been killed.
    }

    private fun startForegroundLocationService() {
        try {
            startForeground(NOTIFICATION_ID, getServiceNotification())
            if (locationUpdatesJob == null) {
                collectLocationUpdates()
            }
        } catch (exception: Exception) {
            Log.e("LocationService", "Failed to start foreground service", exception)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun collectLocationUpdates() {
        locationUpdatesJob = locationProvider
            .locationFlow()
            .mapLatest { location ->
                Log.d("LocationService", "New location received: $location")
                serviceEventMessenger.updateLocation(location)
            }
            .launchIn(scope = applicationScope)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d("LocationService", "Stopping location service")
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
        super.onDestroy()
    }

    private fun checkPermissions() {
        val fineLocationPermission = PermissionChecker.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (fineLocationPermission != PermissionChecker.PERMISSION_GRANTED) {
            stopSelf() // TODO inform user?
        }

        // TODO do we need it?
//        if (PermissionChecker.checkSelfPermission(
//                this,
//                Manifest.permission.POST_NOTIFICATIONS
//        ) != PermissionChecker.PERMISSION_GRANTED
//        ) {
//            stopSelf() // TODO inform user?
//        }
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
            0,
            stopSelfIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // The pending intent that leads to a call to the activity.
        val activityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("You are tracking your path")
            .addAction(0, "Stop tracking", stopSelfPendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(activityPendingIntent)
            .setColorized(true)
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
