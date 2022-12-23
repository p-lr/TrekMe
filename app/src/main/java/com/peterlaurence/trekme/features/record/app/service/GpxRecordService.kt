package com.peterlaurence.trekme.features.record.app.service

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
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.appName
import com.peterlaurence.trekme.core.georecord.data.mapper.gpxToDomain
import com.peterlaurence.trekme.core.georecord.domain.logic.TrackStatCalculator
import com.peterlaurence.trekme.core.georecord.domain.logic.distanceCalculatorFactory
import com.peterlaurence.trekme.core.georecord.domain.logic.mergeBounds
import com.peterlaurence.trekme.core.georecord.domain.logic.mergeStats
import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.events.recording.LiveRoutePause
import com.peterlaurence.trekme.events.recording.LiveRoutePoint
import com.peterlaurence.trekme.events.recording.LiveRouteStop
import com.peterlaurence.trekme.features.record.app.service.event.GpxFileWriteEvent
import com.peterlaurence.trekme.util.getBitmapFromDrawable
import com.peterlaurence.trekme.core.lib.gpx.model.*
import com.peterlaurence.trekme.core.lib.gpx.writeGpx
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.features.record.domain.model.GpxRecordState
import com.peterlaurence.trekme.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.random.Random.Default.nextInt


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

    private var state: GpxRecordState
        get() = eventsGpx.serviceState.value
        set(value) {
            eventsGpx.setServiceState(value)
        }

    private var serviceLooper: Looper? = null
    private var serviceHandler: Handler? = null
    private var locationCounter: Long = 0

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val trackStatCalculatorList = mutableListOf<TrackStatCalculator>()
    private val trackStatCalculator: TrackStatCalculator
        get() = trackStatCalculatorList.lastOrNull() ?: run {
            val calculator = makeTrackStatCalculator()
            trackStatCalculatorList.add(calculator)
            calculator
        }

    /**
     * Prepare the stat calculator.
     * Since we're getting elevations from the GPS, we're using a distance calculator designed to
     * deal with non-trusted elevations.
     */
    private fun makeTrackStatCalculator() = TrackStatCalculator(distanceCalculatorFactory(false))

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

        /* Listen to location data */
        scope.launch {
            locationSource.locationFlow.collect {
                onLocationUpdate(it)
            }
        }

        /* Listen to signals */
        eventsGpx.stopRecordingSignal.map { createGpx() }.launchIn(scope)
        eventsGpx.pauseRecordingSignal.map { pause() }.launchIn(scope)
        eventsGpx.resumeRecordingSignal.map { resume() }.launchIn(scope)
    }

    private fun onLocationUpdate(location: Location) {
        locationCounter++

        /* Drop the first 3 points, so the GPS stabilizes */
        if (locationCounter <= 3) {
            return
        }

        val trackPoint = TrackPoint(
            location.latitude,
            location.longitude, location.altitude, location.time, ""
        )
        if (state == GpxRecordState.STARTED || state == GpxRecordState.RESUMED) {
            eventsGpx.addPointToLiveRoute(trackPoint)
            trackStatCalculator.addTrackPoint(trackPoint.latitude, trackPoint.longitude, trackPoint.elevation, trackPoint.time)
            eventsGpx.postGeoStatistics(getStatistics())
        }
    }

    private fun createNewTrackSegment() {
        trackStatCalculatorList.add(makeTrackStatCalculator())
    }

    /**
     * When we stop recording the location events, create a [Gpx] object for further
     * serialization.
     * Whatever the outcome of this process, a [GpxFileWriteEvent] is emitted in the
     * [THREAD_NAME] thread.
     */
    private fun createGpx() {
        fun generateTrackId(dateStr: String): String {
            return dateStr + '-' + nextInt(0, 1000)
        }

        serviceHandler?.post {
            eventsGpx.stopLiveRoute()

            val trkSegList = ArrayList<TrackSegment>()
            var trackPoints = mutableListOf<TrackPoint>()
            for (event in eventsGpx.liveRouteFlow.replayCache) {
                when (event) {
                    LiveRoutePause, LiveRouteStop -> {
                        if (trackPoints.isNotEmpty()) {
                            trkSegList.add(TrackSegment(trackPoints, UUID.randomUUID().toString()))
                        }
                        trackPoints = mutableListOf()
                    }
                    is LiveRoutePoint -> trackPoints.add(event.pt)
                }
            }

            /* Name the track using the current date */
            val date = Date()
            val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH'h'mm-ss's'", Locale.ENGLISH)
            val trackName = "track-" + dateFormat.format(date)
            val id = generateTrackId(trackName)

            val track = Track(trkSegList, trackName, id = id)
            val bounds = trackStatCalculatorList.mergeBounds()

            /* Make the metadata. We indicate the source of elevation is the GPS, regardless of the
             * actual source (which might be wifi, etc), with a sampling of 1 since each point has
             * its own elevation value. Note that GPS elevation isn't considered trustworthy. */
            val metadata = Metadata(
                trackName,
                date.time,
                bounds, // This isn't mandatory to put this into the metadata, but since we can..
                elevationSourceInfo = GpxElevationSourceInfo(GpxElevationSource.GPS, 1)
            )

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

                /* Now that the file is written, send an event to the application */
                val boundingBox = bounds?.let {
                    BoundingBox(it.minLat, it.maxLat, it.minLon, it.maxLon)
                }
                val geoRecord = gpxToDomain(gpx)
                eventsGpx.postGpxFileWriteEvent(GpxFileWriteEvent(gpxFile, geoRecord, boundingBox))
            } catch (e: Exception) {
                eventBus.postMessage(StandardMessage(getString(R.string.service_gpx_error)))
            } finally {
                stop()
            }
        }
    }

    private fun getStatistics(): GeoStatistics {
        return if (trackStatCalculatorList.size > 1) {
            trackStatCalculatorList.mergeStats()
        } else {
            trackStatCalculator.getStatistics()
        }
    }

    /**
     * Called when the service is started.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

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

        state = GpxRecordState.STARTED

        return START_NOT_STICKY
    }

    /**
     * Stop the service and send the status.
     */
    private fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        eventsGpx.resetLiveRoute()
        eventsGpx.postGeoStatistics(null)
        state = GpxRecordState.STOPPED
        stopSelf()
    }

    private fun pause() {
        eventsGpx.pauseLiveRoute()
        state = GpxRecordState.PAUSED
        createNewTrackSegment()
    }

    private fun resume() {
        if (state == GpxRecordState.PAUSED) {
            state = GpxRecordState.RESUMED
        }
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
