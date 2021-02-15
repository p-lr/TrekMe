package com.peterlaurence.trekme.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.peterlaurence.trekme.MainActivity
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.appName
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.events.StandardMessage
import com.peterlaurence.trekme.core.track.DistanceCalculatorImpl
import com.peterlaurence.trekme.core.track.TrackStatCalculator
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.events.recording.LiveRoutePoint
import com.peterlaurence.trekme.repositories.location.Location
import com.peterlaurence.trekme.repositories.location.LocationSource
import com.peterlaurence.trekme.service.event.GpxFileWriteEvent
import com.peterlaurence.trekme.util.getBitmapFromDrawable
import com.peterlaurence.trekme.util.gpx.model.*
import com.peterlaurence.trekme.util.gpx.writeGpx
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
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
    lateinit var trekMeContext: TrekMeContext

    @Inject
    lateinit var eventsGpx: GpxRecordEvents

    @Inject
    lateinit var eventBus: AppEventBus

    @Inject
    lateinit var locationSource: LocationSource

    private var serviceLooper: Looper? = null
    private var serviceHandler: Handler? = null
    private var locationCounter: Long = 0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var trackStatCalculator: TrackStatCalculator? = null

    override fun onCreate() {
        super.onCreate()

        /* Start up the thread for background execution of tasks withing the service.  Note that we
         * create a separate thread because the service normally runs in the process's main thread,
         * which we don't want to block.
         * We also make it low priority so CPU-intensive work will not disrupt our UI.
         */
        val thread = HandlerThread(THREAD_NAME, Thread.MIN_PRIORITY)
        thread.start()

        /* Get the HandlerThread's Looper and use it for our Handler */
        val looper = thread.looper
        serviceLooper = looper
        serviceHandler = Handler(looper)

        /* Prepare the stat calculator. Since we're getting elevations from the GPS, we're using
         * a distance calculator designed to deal with non-trusted elevations. */
        trackStatCalculator = TrackStatCalculator(DistanceCalculatorImpl(false))

        /* Listen to location data */
        scope.launch {
            locationSource.locationFlow.collect {
                onLocationUpdate(it)
            }
        }

        /* Register a subscriber coroutine to the stop signal */
        scope.launch {
            eventsGpx.stopRecordingSignal.collect {
                createGpx()
            }
        }
    }

    private fun onLocationUpdate(location: Location) {
        locationCounter++

        /* Drop the first 3 points, so the GPS stabilizes */
        if (locationCounter <= 3) {
            return
        }

        val altitude = if (location.altitude != 0.0) location.altitude else null
        val trackPoint = TrackPoint(location.latitude,
                location.longitude, altitude, location.time, "")
        eventsGpx.addTrackPoint(trackPoint)
        trackStatCalculator?.addTrackPoint(trackPoint)
        trackStatCalculator?.getStatistics()?.also { stats ->
            eventsGpx.postTrackStatistics(stats)
        }
    }

    /**
     * When we stop recording the location events, create a [Gpx] object for further
     * serialization.
     * Whatever the outcome of this process, a [GpxFileWriteEvent] is emitted in the
     * [THREAD_NAME] thread.
     */
    private fun createGpx() {
        serviceHandler?.post {
            val trkSegList = ArrayList<TrackSegment>()
            val trackPoints = eventsGpx.liveRouteFlow.replayCache.mapNotNull {
                if (it is LiveRoutePoint) it.pt else null
            }
            trkSegList.add(TrackSegment(trackPoints))

            /* Name the track using the current date */
            val date = Date()
            val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH'h'mm-ss's'", Locale.ENGLISH)
            val trackName = "track-" + dateFormat.format(date)

            val track = Track(trkSegList, trackName)
            track.statistics = trackStatCalculator?.getStatistics()

            /* Make the metadata. We indicate the source of elevation is the GPS, regardless of the
             * actual source (which might be wifi, etc. It doesn't matter because GPS elevation is
             * considered not trustworthy), with a sampling of 1 since each point has its own
             * elevation value. */
            val metadata = Metadata(trackName, date.time, trackStatCalculator?.getBounds(),
                    elevationSourceInfo = ElevationSourceInfo(ElevationSource.GPS, 1))

            val trkList = ArrayList<Track>()
            trkList.add(track)

            val wayPoints = ArrayList<TrackPoint>()

            val gpx = Gpx(metadata, trkList, wayPoints, appName, GPX_VERSION)
            try {
                val gpxFileName = "$trackName.gpx"
                val recordingsDir = trekMeContext.recordingsDir
                        ?: error("Recordings dir is mandatory")
                val gpxFile = File(recordingsDir, gpxFileName)
                val fos = FileOutputStream(gpxFile)
                writeGpx(gpx, fos)
                eventsGpx.postGpxFileWriteEvent(GpxFileWriteEvent(gpxFile, gpx))
            } catch (e: Exception) {
                eventBus.postMessage(StandardMessage(getString(R.string.service_gpx_error)))
            } finally {
                stop()
            }
        }
    }

    /**
     * Called when the service is started.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

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
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val mChannel = NotificationChannel(NOTIFICATION_ID, getText(R.string.service_gpx_record_name), NotificationManager.IMPORTANCE_DEFAULT)
            mChannel.enableLights(true)
            mChannel.lightColor = Color.MAGENTA
            notificationManager.createNotificationChannel(mChannel)
            notificationBuilder.setChannelId(NOTIFICATION_ID)
        }
        val notification = notificationBuilder.build()

        startForeground(SERVICE_ID, notification)

        eventsGpx.setServiceState(true)

        return START_NOT_STICKY
    }

    /**
     * Stop the service and send the status.
     */
    private fun stop() {
        eventsGpx.resetLiveRoute()
        eventsGpx.postTrackStatistics(null)
        eventsGpx.setServiceState(false)
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onDestroy() {
        serviceLooper?.quitSafely()
        scope.cancel()
    }

    companion object {
        private const val GPX_VERSION = "1.1"
        private const val NOTIFICATION_ID = "peterlaurence.GpxRecordService"
        private const val SERVICE_ID = 126585
    }
}

private const val THREAD_NAME = "GpxRecordServiceThread"
