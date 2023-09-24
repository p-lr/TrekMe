package com.peterlaurence.trekme.features.record.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.features.record.data.datasource.LocationSerializerImpl
import com.peterlaurence.trekme.features.record.data.datasource.createSinkFile
import com.peterlaurence.trekme.features.record.domain.model.GpxRecordStateOwner
import com.peterlaurence.trekme.features.record.domain.model.GpxRecorder
import com.peterlaurence.trekme.main.MainActivity
import com.peterlaurence.trekme.util.getBitmapFromDrawable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject


/**
 * A [Started service](https://developer.android.com/guide/components/services.html#CreatingAService)
 * to perform Gpx recordings, even if the user is not interacting with the main application.
 * It is started in the foreground to avoid Android 8.0 (API lvl 26)
 * [background execution limits](https://developer.android.com/about/versions/oreo/background.html).
 * So when there is a Gpx recording, the user can always see it with the icon on the upper left
 * corner of the device.
 *
 * @author P.Laurence on 17/12/17 -- converted to Kotlin on 20/04/19
 */
@AndroidEntryPoint
class GpxRecordService : Service() {
    @Inject
    lateinit var gpxRecordStateOwner: GpxRecordStateOwner

    @Inject
    lateinit var eventsGpx: GpxRecordEvents

    @Inject
    lateinit var excursionRepository: ExcursionRepository

    @Inject
    lateinit var appEventBus: AppEventBus

    @Inject
    lateinit var locationSource: LocationSource

    @Inject
    lateinit var settings: Settings

    private var gpxRecorder: GpxRecorder? = null
    private var sinkFile: File? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        /* Listen to signals */
        eventsGpx.stopRecordingSignal.map {
            stop()
        }.launchIn(scope)

        eventsGpx.pauseRecordingSignal.map {
            pause()
        }.launchIn(scope)
        eventsGpx.resumeRecordingSignal.map {
            gpxRecorder?.resume()
        }.launchIn(scope)
    }

    /**
     * Called when the service is started.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val iconDrawable = ContextCompat.getDrawable(applicationContext, R.mipmap.ic_launcher)
        val icon = if (iconDrawable != null) getBitmapFromDrawable(iconDrawable) else null

        val notificationBuilder = NotificationCompat.Builder(applicationContext, NOTIFICATION_ID)
            .setContentTitle(getText(R.string.app_name))
            .setContentText(getText(R.string.service_gpx_record_action))
            .setSmallIcon(R.drawable.ic_my_location_black_24dp)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .apply {
                if (icon != null) {
                    setLargeIcon(icon)
                }
            }

        if (Build.VERSION.SDK_INT >= 26) {
            /* This is only needed on Devices on Android O and above */
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val mChannel = NotificationChannel(
                NOTIFICATION_ID,
                getText(R.string.service_gpx_record_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            mChannel.enableLights(true)
            mChannel.lightColor = Color.MAGENTA
            notificationManager.createNotificationChannel(mChannel)
            notificationBuilder.setChannelId(NOTIFICATION_ID)
        }
        val notification = notificationBuilder.build()

        startForeground(SERVICE_ID, notification)

        scope.launch {
            /* Create the temp file in which locations will be written. If the application crashes,
             * the gpx won't be created. In this case, that temp file can be used to create the gpx
             * on next startup. */
            sinkFile = createSinkFile(settings)
            val outputStream = withContext(Dispatchers.IO) {
                runCatching { FileOutputStream(sinkFile) }.getOrNull()
            }
            val locationSerializer = if (outputStream != null) {
                LocationSerializerImpl(outputStream)
            } else null

            gpxRecorder = GpxRecorder(
                gpxRecordStateOwner = gpxRecordStateOwner,
                eventsGpx = eventsGpx,
                excursionRepository = excursionRepository,
                locationSource = locationSource,
                locationsSerializer = locationSerializer
            ).also {
                it.start(scope)
            }
        }

        return START_NOT_STICKY
    }

    private fun pause() {
        scope.launch {
            gpxRecorder?.pause()
        }
    }

    /**
     * Stop the service and send the status.
     */
    private fun stop() {
        scope.launch {
            val success = gpxRecorder?.stop()
            if (success != null && success) {
                /* Only delete the sink file when the gpx file was successfully created */
                runCatching { sinkFile?.delete() }
            }
        }.invokeOnCompletion {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        scope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = "peterlaurence.GpxRecordService"
        private const val SERVICE_ID = 126585
    }
}
