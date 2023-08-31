package com.peterlaurence.trekme.features.map.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.os.*
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.features.common.presentation.ui.theme.md_theme_light_primary
import com.peterlaurence.trekme.features.map.domain.core.TrackVicinityAlgorithm
import com.peterlaurence.trekme.features.map.domain.models.TrackFollowServiceState
import com.peterlaurence.trekme.features.map.domain.models.TrackFollowServiceStopEvent
import com.peterlaurence.trekme.features.map.domain.repository.TrackFollowRepository
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.main.MainActivity
import com.peterlaurence.trekme.util.getBitmapFromDrawable
import com.peterlaurence.trekme.util.throttle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * A [Started service](https://developer.android.com/guide/components/services.html#CreatingAService)
 * to send alerts when the user's current location is distant enough to a track, even when
 * the user is not interacting with the main application.
 *
 * The service can be manually stopped using the action from the notification. Or, when the user
 * revoked the notification permission, it can be stopped directly from Android UI panel.
 */
@AndroidEntryPoint
class TrackFollowService : Service() {
    private val notificationChannelId = "peterlaurence.TrackFollowService"
    private val notificationId = 124561

    companion object {
        const val stopAction = "stop"
    }

    private lateinit var onTapPendingIntent: PendingIntent
    private lateinit var onStopPendingIntent: PendingIntent

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    private var sound: MediaPlayer? = null

    @Inject
    lateinit var appEventBus: AppEventBus

    @Inject
    lateinit var mapFeatureEvents: MapFeatureEvents

    @Inject
    lateinit var trackFollowRepository: TrackFollowRepository

    @Inject
    lateinit var locationSource: LocationSource

    var data: TrackFollowRepository.ServiceData? = null

    private var vib: Vibrator? = null
    private val vibrationPattern = longArrayOf(0, 300, 500, 300, 500, 300, 500)
    private val vibrationAmplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0)

    override fun onCreate() {
        super.onCreate()

        val notificationIntent = Intent(this, MainActivity::class.java)
        onTapPendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, TrackFollowService::class.java)
        stopIntent.action = stopAction
        onStopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )

        notificationManager = NotificationManagerCompat.from(this)

        if (!notificationManager.areNotificationsEnabled() && Build.VERSION.SDK_INT >= 33) {
            appEventBus.requestNotificationPermission()
        }

        sound = MediaPlayer.create(applicationContext, R.raw.stop)

        vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as? Vibrator
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
            .setContentText(getText(R.string.service_track_follow_name))
            .setSmallIcon(R.drawable.transit_detour)
            .setContentIntent(onTapPendingIntent)
            .setColor(md_theme_light_primary.toArgb())
            .addAction(
                R.drawable.ic_stop_black_24dp,
                getString(R.string.service_track_follow_stop),
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
        if (Build.VERSION.SDK_INT >= 26) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val chan = NotificationChannel(
                notificationChannelId,
                getText(R.string.service_track_follow_notification_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            chan.enableLights(true)
            chan.lightColor = Color.MAGENTA
            notificationManager.createNotificationChannel(chan)
            notificationBuilder.setChannelId(notificationChannelId)
        }

        startForeground(notificationId, notificationBuilder.build())

        job = scope.launch {
            val data = trackFollowRepository.serviceData.receive()
            this@TrackFollowService.data = data

            /* Notify the rest the app that the service is started */
            trackFollowRepository.serviceState.value = TrackFollowServiceState.Started(data.mapId, data.trackId)
            val algorithm = TrackVicinityAlgorithm(data.verifier)

            processLocation(algorithm)
        }

        return START_NOT_STICKY
    }

    private suspend fun processLocation(algorithm: TrackVicinityAlgorithm) {
        locationSource.locationFlow.throttle(5000).collect { loc ->
            val shouldAlert = algorithm.processLocation(loc)

            if (shouldAlert) {
                coroutineScope {
                    launch {
                        repeat(3) {
                            sound?.start()
                            delay(500)
                        }
                    }
                    launch {
                        vibrate(vibrationPattern)
                    }
                }
            }
        }
    }

    private fun vibrate(pattern: LongArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib?.vibrate(VibrationEffect.createWaveform(pattern, vibrationAmplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vib?.vibrate(pattern, -1)
        }
    }

    private fun stopService() {
        scope.cancel()
        sound?.release()
        trackFollowRepository.serviceState.value = TrackFollowServiceState.Stopped
        data?.also {
            mapFeatureEvents.postTrackFollowStopEvent(
                TrackFollowServiceStopEvent(it.mapId, it.trackId)
            )
        }
        stopSelf()
    }
}