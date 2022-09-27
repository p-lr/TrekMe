package com.peterlaurence.trekme.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.dao.MapDownloadDao
import com.peterlaurence.trekme.core.map.domain.interactors.SaveMapInteractor
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.repositories.download.DownloadRepository
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.features.common.domain.interactors.georecord.ImportGeoRecordInteractor
import com.peterlaurence.trekme.main.MainActivity
import com.peterlaurence.trekme.service.event.*
import com.peterlaurence.trekme.util.getBitmapFromDrawable
import com.peterlaurence.trekme.util.stackTraceToString
import com.peterlaurence.trekme.util.throttle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject


/**
 * A [Started service](https://developer.android.com/guide/components/services.html#CreatingAService)
 * to perform map download, even if the user is not interacting with the main application.
 *
 * It is started in the foreground to avoid Android 8.0 (API lvl 26)
 * [background execution limit](https://developer.android.com/about/versions/oreo/background.html).
 * So when there is a map download, the user can always see it with the icon on the upper left
 * corner of the device.
 */
@AndroidEntryPoint
class DownloadService : Service() {
    private val notificationChannelId = "peterlaurence.DownloadService"
    private val downloadServiceNofificationId = 128565
    private val stopAction = "stop"

    @Inject
    lateinit var mapDownloadDao: MapDownloadDao

    @Inject
    lateinit var saveMapInteractor: SaveMapInteractor

    @Inject
    lateinit var repository: DownloadRepository

    @Inject
    lateinit var appEventBus: AppEventBus

    @Inject
    lateinit var importGeoRecordInteractor: ImportGeoRecordInteractor

    @Inject
    lateinit var app: Application

    private lateinit var onTapPendingIntent: PendingIntent
    private lateinit var onStopPendingIntent: PendingIntent

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManagerCompat

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val progressEvent = MapDownloadPending(0)

    companion object {
        private val _started = MutableStateFlow(false)
        val started = _started.asStateFlow()
    }

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
        /* If the user used the notification action-stop button, stop the service */
        if (intent.action == stopAction) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopService()
            return START_NOT_STICKY
        }

        /* Notify that a download is already running */
        if (started.value) {
            repository.postDownloadEvent(MapDownloadAlreadyRunning)
            return START_NOT_STICKY
        }

        val iconDrawable = ContextCompat.getDrawable(applicationContext, R.mipmap.ic_launcher)
        val icon = if (iconDrawable != null) getBitmapFromDrawable(iconDrawable) else null

        /* From here, we know that the service is being created by the activity */
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

        startForeground(downloadServiceNofificationId, notificationBuilder.build())

        /* Get ready for download and request download spec */
        progressEvent.progress = 0

        scope.launch {
            /* Only process the first event */
            val request = repository.getDownloadMapRequest()
            if (request != null) {
                processRequestDownloadMapEvent(request)
            }
        }

        _started.value = true
        appEventBus.postMessage(StandardMessage(getString(R.string.download_confirm)))

        return START_NOT_STICKY
    }

    private suspend fun processRequestDownloadMapEvent(request: DownloadMapRequest) {
        val throttledTask = scope.throttle(1000) { p: Int ->
            onDownloadProgress(p)
        }

        when (val result =
            mapDownloadDao.processRequest(
                request,
                onProgress = { p -> throttledTask.trySend(p) }
            )
        ) {
            is MapDownloadDao.MapDownloadResult.Error -> {
                repository.postDownloadEvent(MapDownloadStorageError)
                stopService()
            }
            is MapDownloadDao.MapDownloadResult.Success -> {
                postProcess(result.map, request.geoRecordUris)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private fun onDownloadProgress(progress: Int) {
        if (progress == 100) {
            notificationBuilder.setOngoing(false)
            notificationBuilder.setProgress(0, 0, false)
            notificationBuilder.setContentText(getText(R.string.service_download_finished))
            notificationBuilder.mActions.clear()
        } else {
            notificationBuilder.setProgress(100, progress, false)
        }
        try {
            notificationManager.notify(downloadServiceNofificationId, notificationBuilder.build())
        } catch (e: RuntimeException) {
            // can't figure out why it's (rarely) thrown. Log it for now
            Log.e(TAG, stackTraceToString(e))
        }

        /* Send a message carrying the progress info */
        progressEvent.progress = progress
        repository.postDownloadEvent(progressEvent)
    }

    private fun postProcess(map: Map, geoRecordUris: Set<Uri>) {
        scope.launch {
            saveMapInteractor.addAndSaveMap(map)
            geoRecordUris.forEach { uri ->
                importGeoRecordInteractor.applyGeoRecordUriToMap(uri, app.contentResolver, map)
            }

            /* Notify that the download is finished correctly.
             * Don't attempt to send more notifications, they will be dismissed anyway since the
             * service is about to stop. */
            repository.postDownloadEvent(MapDownloadFinished(map.id))

            stopService()
        }
    }

    private fun stopService() {
        _started.value = false
        scope.cancel()
        stopSelf()
    }
}

private const val TAG = "DownloadService.kt"
