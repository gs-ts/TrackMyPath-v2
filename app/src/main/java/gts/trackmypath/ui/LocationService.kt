package gts.trackmypath.ui

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
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
import gts.trackmypath.domain.LocationHolder
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
    lateinit var locationHolder: LocationHolder

    private lateinit var notificationManager: NotificationManager

    init {
        Log.d("LocationService", "Init $this")
    }

    private var locationUpdatesJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        checkPermissions()
        startForeground(NOTIFICATION_ID, getNotification())
        if (locationUpdatesJob == null) {
            collectLocationUpdates()
        }
        return START_NOT_STICKY // Tells the system not to recreate the service after it's been killed.
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun collectLocationUpdates() {
        locationUpdatesJob = locationProvider
            .locationFlow()
            .mapLatest { location ->
                Log.d("LocationService", "New location received: $location")
                locationHolder.updateLocation(location)
            }
            .launchIn(scope = applicationScope)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.d("LocationService", "stopService!")
        locationUpdatesJob?.cancel()
        locationUpdatesJob = null
        super.onDestroy()
    }

    private fun checkPermissions() {
        val fineLocationPermission = PermissionChecker.checkSelfPermission(this, ACCESS_FINE_LOCATION)
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
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, importance).apply {
            description = "something..."
        }
        val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun getNotification(): Notification {
        val intent = Intent(this, LocationService::class.java)
        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra("started_from_notification", true)
        // The PendingIntent that leads to a call to onStartCommand() in this service.
        val servicePendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            FLAG_MUTABLE
        )

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .addAction(0, "Stop tracking", servicePendingIntent)
            .setContentTitle("You are tracking your path")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)

        return builder.build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "track_my_path_notification_channel"
        private const val NOTIFICATION_CHANNEL_NAME = "track my path"
    }
}
