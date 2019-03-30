package com.peterlaurence.trekme.core.track

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.peterlaurence.trekme.core.TrekMeContext.recordingsDir
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.gpx.GPXParser
import com.peterlaurence.trekme.util.gpx.GPXWriter
import com.peterlaurence.trekme.util.gpx.model.Gpx
import com.peterlaurence.trekme.util.gpx.model.Track
import com.peterlaurence.trekme.util.gpx.model.TrackPoint
import com.peterlaurence.trekme.util.gpx.model.TrackSegment
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import java.io.*

/**
 * Utility toolbox to :
 *
 *  * Import a gpx track file into a [Map].
 *  * Get the list of gpx files created by location recording.
 *  * Get the content of each gpx file as [Gpx] instances.
 *
 * @author peterLaurence on 03/03/17 -- converted to Kotlin on 16/09/18
 */
object TrackImporter {
    const val TAG = "TrackImporter"
    /**
     * Get the list of [File] which extension is in the list of supported extension for track
     * file. Files are searched into the
     * [com.peterlaurence.trekme.core.TrekMeContext.recordingsDir].
     */
    val recordings: Array<File>?
        get() = recordingsDir.listFiles(SUPPORTED_FILE_FILTER)

    private val recordingsToGpx: kotlin.collections.MutableMap<File, Gpx> = mutableMapOf()

    /**
     * In the context of this call, new recordings have been added, or this is the first time
     * this function is called in the lifecycle of the  app.
     * The list of recordings, [recordings], is considered up to date. The map between each
     * recording and its corresponding parsed object, [recordingsToGpx], needs to be updated.
     * The first call parses all recordings. Subsequent calls only parse new files.
     * This is a blocking call, so it should be called inside a coroutine.
     */
    fun getRecordingsToGpxMap(): kotlin.collections.Map<File, Gpx> {
        if (recordingsToGpx.isEmpty()) {
            recordings?.forEach {
                try {
                    val gpx = GPXParser.parse(FileInputStream(it))
                    recordingsToGpx[it] = gpx
                } catch (e: Exception) {
                    Log.e(TAG, "The file ${it.name} was parsed with an error")
                }
            }
        } else {
            recordings?.filter { !recordingsToGpx.keys.contains(it) }?.forEach {
                try {
                    val gpx = GPXParser.parse(FileInputStream(it))
                    recordingsToGpx[it] = gpx
                } catch (e: Exception) {
                    Log.e(TAG, "The file ${it.name} was parsed with an error")
                }
            }
            recordingsToGpx.keys.filter { !(recordings?.contains(it) ?: false) }.forEach {
                recordingsToGpx.remove(it)
            }
        }
        return recordingsToGpx.toMap()
    }

    /**
     * The user may have imported a regular gpx file (so it doesn't have any statistics).
     * In this call, we consider that each gpx file has already been parsed, and that the
     * [recordingsToGpx] Map is up to date. So typically, [getRecordingsToGpxMap] should be called
     * first.
     * First, we calculate the statistics for the first track.
     * If the [GPXParser] read statistics for this track, we check is there is any difference
     * (because the statistics calculation is subjected to be adjusted frequently), we update the
     * gpx file.
     */
    fun computeStatistics(): kotlin.collections.Map<File, Gpx> {
        recordingsToGpx.forEach {
            val statCalculator = TrackStatCalculator()
            it.value.tracks.firstOrNull()?.let { track ->
                track.trackSegments.forEach { trackSegment ->
                    trackSegment.hpFilter()
                    statCalculator.addTrackPointList(trackSegment.trackPoints)
                }

                val updatedStatistics = statCalculator.getStatistics()
                if (track.statistics != null && track.statistics != updatedStatistics) {
                    /* Track statistics have changed, update the file */
                    track.statistics = updatedStatistics
                    val fos = FileOutputStream(it.key)
                    GPXWriter.write(it.value, fos)
                }
                track.statistics = updatedStatistics
            }
        }

        return recordingsToGpx.toMap()
    }

    private val supportedTrackFilesExtensions = arrayOf("gpx", "xml")

    private val SUPPORTED_FILE_FILTER = filter@{ dir: File, filename: String ->
        /* We only look at files */
        if (File(dir, filename).isDirectory) {
            return@filter false
        }

        supportedTrackFilesExtensions.any { filename.endsWith(".$it") }
    }

    fun isFileSupported(uri: Uri): Boolean {
        val path = uri.path
        val extension = path?.substring(path.lastIndexOf(".") + 1) ?: ""

        if ("" == extension) return false

        return supportedTrackFilesExtensions.any { it == extension }
    }

    /**
     * Applies the GPX content given as an [Uri] to the provided [Map].
     */
    fun CoroutineScope.applyGpxUriToMapAsync(uri: Uri, contentResolver: ContentResolver, map: Map): Deferred<GpxParseResult> {
        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
        return parcelFileDescriptor?.let {
            val fileDescriptor = parcelFileDescriptor.fileDescriptor
            val fileInputStream = FileInputStream(fileDescriptor)
            val fileName = FileUtils.getFileRealPathFromURI(contentResolver, uri) ?: "A track"
            applyGpxInputStreamToMapAsync(fileInputStream, map, fileName) {
                it.close()
            }
        } ?: throw FileNotFoundException("File with uri $uri doesn't exists")
    }

    fun CoroutineScope.applyGpxFileToMap(file: File, map: Map) = launch {
        try {
            val fileInputStream = FileInputStream(file)
            applyGpxInputStreamToMapAsync(fileInputStream, map, file.name).await()
        } catch (e: Exception) {
            /* Don't care */
        }
    }

    data class GpxParseResult(val map: Map, val routes: List<RouteGson.Route>, val wayPoints: List<MarkerGson.Marker>,
                              val newRouteCount: Int, val newMarkersCount: Int)

    /**
     * Parses the GPX content provided as [InputStream], off UI thread.
     */
    private fun CoroutineScope.readGpxInputStreamAsync(input: InputStream, map: Map, defaultName: String) = async(Dispatchers.Default) {
        GPXParser.parseSafely(input)?.let { gpx ->
            val routes = gpx.tracks.mapIndexed { index, track ->
                gpxTrackToRoute(map, track, index, defaultName)
            }
            val waypoints = gpx.wayPoints.mapIndexed { index, wpt ->
                gpxWaypointsToMarker(map, wpt, index, defaultName)
            }
            Pair(routes, waypoints)
        }
    }

    /**
     * Launches a GPX parse. Then, on the calling [CoroutineScope] (which [CoroutineDispatcher] should
     * be [Dispatchers.Main]), applies the result on the provided [Map].
     */
    private fun CoroutineScope.applyGpxInputStreamToMapAsync(input: InputStream, map: Map,
                                                             defaultName: String,
                                                             afterParseCallback: (() -> Unit)? = null) = async {
        val deferred = readGpxInputStreamAsync(input, map, defaultName)

        /* Whatever the caller will do with the parse result, we want to apply it to the map and add
         * additional info
         */
        val pair = deferred.await()
        afterParseCallback?.let { it() }

        if (pair != null) {
            return@async applyGpxParseResultToMap(map, pair.first, pair.second)
        } else {
            throw GpxParseException()
        }
    }

    class GpxParseException : Exception()

    private fun applyGpxParseResultToMap(map: Map, routes: List<RouteGson.Route>, wayPoints: List<MarkerGson.Marker>): GpxParseResult {
        val newRouteCount = TrackTools.updateRouteList(map, routes)
        val newMarkersCount = TrackTools.updateMarkerList(map, wayPoints)
        MapLoader.saveRoutes(map)
        MapLoader.saveMarkers(map)

        return GpxParseResult(map, routes, wayPoints, newRouteCount, newMarkersCount)
    }

    /**
     * Converts a [Track] into a [RouteGson.Route].
     * A single [Track] may contain several [TrackSegment].
     */
    private fun gpxTrackToRoute(map: Map, track: Track, index: Int, defaultName: String): RouteGson.Route {
        /* Create a new route */
        val route = RouteGson.Route()

        /* The route name is the track name if it has one. Otherwise we take the default name */
        route.name = if (track.name.isNotEmpty()) {
            track.name
        } else {
            "$defaultName#$index"
        }

        /* The route should be visible by default */
        route.visible = true

        /* All track segments are concatenated */
        val trackSegmentList = track.trackSegments
        for (trackSegment in trackSegmentList) {
            val trackPointList = trackSegment.trackPoints
            for (trackPoint in trackPointList) {
                val marker = MarkerGson.Marker()

                /* If the map uses a projection, store projected values */
                val projectedValues: DoubleArray?
                val projection = map.projection
                if (projection != null) {
                    projectedValues = projection.doProjection(trackPoint.latitude, trackPoint.longitude)
                    if (projectedValues != null) {
                        marker.proj_x = projectedValues[0]
                        marker.proj_y = projectedValues[1]
                    }
                }

                /* In any case, we store the wgs84 coordinates */
                marker.lat = trackPoint.latitude
                marker.lon = trackPoint.longitude

                route.route_markers.add(marker)
            }
        }
        return route
    }

    private fun gpxWaypointsToMarker(map: Map, wpt: TrackPoint, index: Int, defaultName: String): MarkerGson.Marker {
        val marker = MarkerGson.Marker()

        marker.name = if (wpt.name?.isNotEmpty() == true) {
            wpt.name
        } else {
            "$defaultName-wpt${index + 1}"
        }

        /* If the map uses a projection, store projected values */
        val projectedValues: DoubleArray?
        val projection = map.projection
        if (projection != null) {
            projectedValues = projection.doProjection(wpt.latitude, wpt.longitude)
            if (projectedValues != null) {
                marker.proj_x = projectedValues[0]
                marker.proj_y = projectedValues[1]
            }
        }

        /* In any case, we store the wgs84 coordinates */
        marker.lat = wpt.latitude
        marker.lon = wpt.longitude

        return marker
    }
}
