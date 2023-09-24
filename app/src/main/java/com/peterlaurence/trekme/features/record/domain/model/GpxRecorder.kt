package com.peterlaurence.trekme.features.record.domain.model

import com.peterlaurence.trekme.core.appName
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.core.georecord.data.mapper.gpxToDomain
import com.peterlaurence.trekme.core.georecord.domain.logic.TrackStatCalculator
import com.peterlaurence.trekme.core.georecord.domain.logic.distanceCalculatorFactory
import com.peterlaurence.trekme.core.georecord.domain.logic.mergeBounds
import com.peterlaurence.trekme.core.georecord.domain.logic.mergeStats
import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics
import com.peterlaurence.trekme.core.lib.gpx.model.GPX_VERSION
import com.peterlaurence.trekme.core.lib.gpx.model.Gpx
import com.peterlaurence.trekme.core.lib.gpx.model.GpxElevationSource
import com.peterlaurence.trekme.core.lib.gpx.model.GpxElevationSourceInfo
import com.peterlaurence.trekme.core.lib.gpx.model.Metadata
import com.peterlaurence.trekme.core.lib.gpx.model.Track
import com.peterlaurence.trekme.core.lib.gpx.model.TrackPoint
import com.peterlaurence.trekme.core.lib.gpx.model.TrackSegment
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.events.recording.LiveRoutePause
import com.peterlaurence.trekme.events.recording.LiveRoutePoint
import com.peterlaurence.trekme.events.recording.LiveRouteStop
import com.peterlaurence.trekme.features.record.app.service.event.NewExcursionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

class GpxRecorder(
    private val gpxRecordStateOwner: GpxRecordStateOwner,
    private val eventsGpx: GpxRecordEvents,
    val excursionRepository: ExcursionRepository,
    val locationSource: LocationSource,
    private val locationsSerializer: LocationsSerializer?
) {
    private var locationCounter: Long = 0

    private val trackStatCalculatorList = mutableListOf<TrackStatCalculator>()
    private val trackStatCalculator: TrackStatCalculator
        get() = trackStatCalculatorList.lastOrNull() ?: run {
            val calculator = makeTrackStatCalculator()
            trackStatCalculatorList.add(calculator)
            calculator
        }

    private var state: GpxRecordState
        get() = gpxRecordStateOwner.gpxRecordState.value
        set(value) {
            gpxRecordStateOwner.setServiceState(value)
        }

    fun start(scope: CoroutineScope) {
        state = GpxRecordState.STARTED

        /* Listen to location data */
        scope.launch {
            locationSource.locationFlow.collect {
                onLocationUpdate(it)

                if (state == GpxRecordState.STARTED || state == GpxRecordState.RESUMED) {
                    locationsSerializer?.onLocation(it)
                }
            }
        }
    }

    suspend fun pause() {
        eventsGpx.pauseLiveRoute()
        state = GpxRecordState.PAUSED
        createNewTrackSegment()
        locationsSerializer?.pause()
    }

    fun resume() {
        if (state == GpxRecordState.PAUSED) {
            state = GpxRecordState.RESUMED
        }
    }

    /**
     * Stop the service and send the status.
     */
    suspend fun stop(): Boolean {
        val success = createGpx()
        eventsGpx.resetLiveRoute()
        eventsGpx.postGeoStatistics(null)
        state = GpxRecordState.STOPPED
        return success
    }

    /**
     * When we stop recording the location events, create a [Gpx] object for further serialization.
     * Whatever the outcome of this process, a [NewExcursionEvent] is emitted.
     * TODO: this should be done in data layer
     */
    private suspend fun createGpx(): Boolean {
        fun generateTrackId(dateStr: String): String {
            return dateStr + '-' + Random.nextInt(0, 1000)
        }

        return withContext(Dispatchers.IO) {
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
                bounds, // This isn't mandatory to put this into the metadata
                elevationSourceInfo = GpxElevationSourceInfo(GpxElevationSource.GPS, 1)
            )

            val trkList = ArrayList<Track>()
            trkList.add(track)

            val wayPoints = ArrayList<TrackPoint>()

            val gpx = Gpx(metadata, trkList, wayPoints, appName, GPX_VERSION)
            runCatching {
                val boundingBox = bounds?.let {
                    BoundingBox(it.minLat, it.maxLat, it.minLon, it.maxLon)
                }
                val geoRecord = gpxToDomain(gpx, name = trackName)

                val excursionId = UUID.randomUUID().toString()
                val result = excursionRepository.putExcursion(
                    id = excursionId,
                    title = trackName,
                    type = ExcursionType.Hike,
                    description = "",
                    geoRecord = geoRecord
                )

                if (result == ExcursionRepository.PutExcursionResult.Ok) {
                    eventsGpx.postNewExcursionEvent(NewExcursionEvent(excursionId, boundingBox))
                    true
                } else false
            }.getOrElse { false }
        }
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

    private fun getStatistics(): GeoStatistics {
        return if (trackStatCalculatorList.size > 1) {
            trackStatCalculatorList.mergeStats()
        } else {
            trackStatCalculator.getStatistics()
        }
    }

    /**
     * Prepare the stat calculator.
     * Since we're getting elevations from the GPS, we're using a distance calculator designed to
     * deal with non-trusted elevations.
     */
    private fun makeTrackStatCalculator() = TrackStatCalculator(distanceCalculatorFactory(false))

    private fun createNewTrackSegment() {
        trackStatCalculatorList.add(makeTrackStatCalculator())
    }
}
