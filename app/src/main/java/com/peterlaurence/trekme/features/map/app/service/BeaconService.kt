package com.peterlaurence.trekme.features.map.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.location.LocationSource
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.main.MainActivity
import com.peterlaurence.trekme.util.getBitmapFromDrawable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * A [Started service](https://developer.android.com/guide/components/services.html#CreatingAService)
 * to monitor beacons even when the user is not interacting with the main application.
 *
 * The service collects active map changes, and when a map has no beacons, the service stops itself.
 * The service can be manually stopped using the action from the notification. Or, when the user
 * revoked the notification permission, it can be stopped directly from Android UI panel.
 */
@AndroidEntryPoint
class BeaconService : Service() {
    private val notificationChannelId = "peterlaurence.BeaconService"
    private val notificationId = 124563

    companion object {
        const val stopAction = "stop"
    }

    private lateinit var onTapPendingIntent: PendingIntent
    private lateinit var onStopPendingIntent: PendingIntent

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    @Inject
    lateinit var appEventBus: AppEventBus

    @Inject
    lateinit var mapRepository: MapRepository

    @Inject
    lateinit var locationSource: LocationSource

    override fun onCreate() {
        super.onCreate()

        val notificationIntent = Intent(this, MainActivity::class.java)
        onTapPendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, BeaconService::class.java)
        stopIntent.action = stopAction
        onStopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

        notificationManager = NotificationManagerCompat.from(this)

        if (!notificationManager.areNotificationsEnabled() && android.os.Build.VERSION.SDK_INT >= 33) {
            appEventBus.requestNotificationPermission()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        /* If the user used the stop button in the notification, stop the service */
        if (intent.action == stopAction) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopService()
            return START_NOT_STICKY
        }

        /* If the service is already running, no need to continue */
        if (job?.isActive == true) {
            return START_NOT_STICKY
        }

        val iconDrawable = ContextCompat.getDrawable(applicationContext, R.mipmap.ic_launcher)
        val icon = if (iconDrawable != null) getBitmapFromDrawable(iconDrawable) else null

        notificationBuilder = NotificationCompat.Builder(applicationContext, notificationChannelId)
            .setContentTitle(getText(R.string.app_name))
            .setContentText(getText(R.string.service_beacon_name))
            .setSmallIcon(R.drawable.ic_beacon_24dp)
            .setContentIntent(onTapPendingIntent)
            .setColor(getColor(R.color.colorAccent))
            .addAction(
                R.drawable.ic_stop_black_24dp,
                getString(R.string.service_beacon_stop),
                onStopPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOnlyAlertOnce(true)
            .apply {
                if (icon != null) {
                    setLargeIcon(icon)
                }
            }

        /* This is only needed on Devices on Android O and above */
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val chan = NotificationChannel(
                notificationChannelId,
                getText(R.string.service_beacon_notification_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            chan.enableLights(true)
            chan.lightColor = Color.MAGENTA
            notificationManager.createNotificationChannel(chan)
            notificationBuilder.setChannelId(notificationChannelId)
        }

        startForeground(notificationId, notificationBuilder.build())

        job = scope.launch {
            mapRepository.currentMapFlow.collectLatest { map ->
                if (map != null) processBeacons(map)
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun processBeacons(map: Map) {
        combine(locationSource.locationFlow, map.beacons) { loc, beacons ->
            if (beacons.isEmpty()) stopService()

            println("xxxxxx got loc for ${map.name}, check in ${map.beacons.value.size} beacons")
            for (beacon in map.beacons.value) {

            }
        }.collect()
    }

    private fun stopService() {
        println("xxxxx stopping service")
        scope.cancel()
        stopSelf()
    }
}