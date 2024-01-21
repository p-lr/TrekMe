package com.peterlaurence.trekme.features.mapcreate.app.service.download

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.interactors.MapDownloadInteractor
import com.peterlaurence.trekme.core.map.domain.models.*
import com.peterlaurence.trekme.features.mapcreate.domain.repository.DownloadRepository
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.main.MainActivity
import com.peterlaurence.trekme.util.getBitmapFromDrawable
import com.peterlaurence.trekme.util.throttle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject


/**
 * A [Started service](https://developer.android.com/guide/components/services.html#CreatingAService)
 * to perform map download, even if the user is not interacting with the main application.
 *
 * It is started in the foreground to avoid Android 8.0 (API lvl 26)
 * [background execution limit](https://developer.android.com/about/versions/oreo/background.html).
 * When there is a map download, if the user has granted the notification permission, the user can
 * see the icon on the upper left corner of the device (and see the download progression from the
 * notifications).
 * If the user revoked the notification permission, the download progression can always be seen from
 * the map list.
 */
@AndroidEntryPoint
class DownloadService : Service() {
    private val notificationChannelId = "peterlaurence.DownloadService"
    private val downloadServiceNotificationId = 128565

    companion object {
        const val stopAction = "stop"
    }

    @Inject
    lateinit var mapDownloadInteractor: MapDownloadInteractor

    @Inject
    lateinit var repository: DownloadRepository

    @Inject
    lateinit var appEventBus: AppEventBus

    private lateinit var onTapPendingIntent: PendingIntent
    private lateinit var onStopPendingIntent: PendingIntent

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        val notificationIntent = Intent(this, MainActivity::class.java)
        onTapPendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, DownloadService::class.java)
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

    /**
     * Called when the service is started.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        /* If the user used the notification action-stop button or used the stop button withing the app, stop the service */
        if (intent.action == stopAction) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopService()
            return START_NOT_STICKY
        }

        /* Notify that a download is already running */
        if (repository.isStarted()) {
            repository.postDownloadEvent(MapDownloadAlreadyRunning)
            return START_NOT_STICKY
        }

        val iconDrawable = ContextCompat.getDrawable(applicationContext, R.mipmap.ic_launcher)
        val icon = if (iconDrawable != null) getBitmapFromDrawable(iconDrawable) else null

        notificationBuilder = NotificationCompat.Builder(applicationContext, notificationChannelId)
            .setContentTitle(getText(R.string.app_name))
            .setContentText(getText(R.string.service_download_action))
            .setSmallIcon(R.drawable.ic_file_download_24dp)
            .setContentIntent(onTapPendingIntent)
            .setColor(getColor(R.color.colorAccent))
            .addAction(
                R.drawable.ic_stop_black_24dp,
                getString(R.string.service_download_stop),
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
                getText(R.string.service_download_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            chan.enableLights(true)
            chan.lightColor = Color.MAGENTA
            notificationManager.createNotificationChannel(chan)
            notificationBuilder.setChannelId(notificationChannelId)
        }

        startForeground(downloadServiceNotificationId, notificationBuilder.build())

        val spec = repository.getMapDownloadSpec()
        if (spec != null) {
            scope.launch {
                processDownloadSpec(spec)
            }

            repository.setStatus(DownloadRepository.Started(spec))
            appEventBus.postMessage(StandardMessage(getString(R.string.download_confirm)))
        }

        return START_NOT_STICKY
    }

    private suspend fun processDownloadSpec(spec: MapDownloadSpec) {
        val throttledTask = scope.throttle(1000) { p: Int ->
            onDownloadProgress(p)
        }

        mapDownloadInteractor.processDownloadSpec(
            spec, onProgress = { p -> throttledTask.trySend(p) }
        )
        onDownloadFinished()

        /* Whatever the outcome, stop the service. Don't attempt to send more notifications, they
         * will be dismissed anyway since the service is about to stop. */
        stopService()
    }

    @SuppressLint("MissingPermission")
    private fun onDownloadProgress(progress: Int) {
        notificationBuilder.setProgress(100, progress, false)
        runCatching {
            // SecurityException thrown when missing permission
            notificationManager.notify(downloadServiceNotificationId, notificationBuilder.build())
        }
    }

    @SuppressLint("RestrictedApi")
    private fun onDownloadFinished() {
        notificationBuilder.setOngoing(false)
        notificationBuilder.setProgress(0, 0, false)
        notificationBuilder.setContentText(getText(R.string.service_download_finished))
        notificationBuilder.mActions.clear()
    }

    private fun stopService() {
        repository.setStatus(DownloadRepository.Stopped)
        scope.cancel()
        stopSelf()
    }
}

private const val TAG = "DownloadService.kt"
