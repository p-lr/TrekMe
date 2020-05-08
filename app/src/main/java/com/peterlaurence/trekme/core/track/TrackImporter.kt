package com.peterlaurence.trekme.core.track

import android.content.ContentResolver
import android.net.Uri
import com.peterlaurence.trekme.core.TrekMeContext.recordingsDir
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.gpx.GPXParser
import com.peterlaurence.trekme.util.gpx.model.Gpx
import com.peterlaurence.trekme.util.gpx.model.Track
import com.peterlaurence.trekme.util.gpx.model.TrackPoint
import com.peterlaurence.trekme.util.gpx.model.TrackSegment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

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
        get() = recordingsDir?.listFiles(SUPPORTED_FILE_FILTER)

    private val supportedTrackFilesExtensions = arrayOf("gpx", "xml")

    private val SUPPORTED_FILE_FILTER = filter@{ dir: File, filename: String ->
        /* We only look at files */
        if (File(dir, filename).isDirectory) {
            return@filter false
        }

        supportedTrackFilesExtensions.any { filename.endsWith(".$it") }
    }

    fun isFileSupported(uri: Uri, contentResolver: ContentResolver): Boolean {
        val fileName = FileUtils.getFileRealFileNameFromURI(contentResolver, uri)
        val extension = fileName.substringAfterLast('.', "")

        if ("" == extension) return false

        return supportedTrackFilesExtensions.any { it == extension }
    }

    /**
     * A [TrackPoint] is a raw point that we make right after a location api callback.
     * To be drawn relatively to a [Map], it must be converted to a [MarkerGson.Marker].
     * This should be called off UI thread.
     */
    fun TrackPoint.toMarker(map: Map): MarkerGson.Marker {
        val marker = MarkerGson.Marker()

        /* If the map uses a projection, store projected values */
        val projectedValues: DoubleArray?
        val projection = map.projection
        if (projection != null) {
            projectedValues = projection.doProjection(latitude, longitude)
            if (projectedValues != null) {
                marker.proj_x = projectedValues[0]
                marker.proj_y = projectedValues[1]
            }
        }

        /* In any case, we store the wgs84 coordinates */
        marker.lat = latitude
        marker.lon = longitude

        /* If we have elevation information, store it */
        marker.elevation = elevation
        return marker
    }

    /**
     * Applies the GPX content given as an [Uri] to the provided [Map].
     */
    suspend fun applyGpxUriToMapAsync(uri: Uri, contentResolver: ContentResolver, map: Map): GpxParseResult {
        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
        return parcelFileDescriptor?.let {
            val fileDescriptor = parcelFileDescriptor.fileDescriptor
            val fileInputStream = FileInputStream(fileDescriptor)
            val fileName = FileUtils.getFileRealFileNameFromURI(contentResolver, uri) ?: "A track"
            applyGpxInputStreamToMapAsync(fileInputStream, map, fileName) {
                it.close()
            }
        } ?: throw FileNotFoundException("File with uri $uri doesn't exists")
    }

    /**
     * Applies the GPX content given as a [File] to the provided [Map].
     */
    suspend fun applyGpxFileToMapAsync(file: File, map: Map): GpxParseResult {
        val fileInputStream = FileInputStream(file)
        return applyGpxInputStreamToMapAsync(fileInputStream, map, file.name)
    }

    data class GpxParseResult(val map: Map, val routes: List<RouteGson.Route>, val wayPoints: List<MarkerGson.Marker>,
                              val newRouteCount: Int, val newMarkersCount: Int)

    /**
     * Parses the GPX content provided as [InputStream], off UI thread.
     */
    private suspend fun readGpxInputStreamAsync(input: InputStream, map: Map, defaultName: String) = withContext(Dispatchers.Default) {
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
    private suspend fun applyGpxInputStreamToMapAsync(input: InputStream, map: Map,
                                                      defaultName: String,
                                                      afterParseCallback: (() -> Unit)? = null): GpxParseResult {
        val pair = readGpxInputStreamAsync(input, map, defaultName)

        /* Whatever the caller will do with the parse result, we want to apply it to the map and add
         * additional info
         */
        afterParseCallback?.let { it() }

        if (pair != null) {
            return applyGpxParseResultToMap(map, pair.first, pair.second)
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
     * This should be call off UI thread.
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
                val marker = trackPoint.toMarker(map)
                route.route_markers.add(marker)
            }
        }
        return route
    }

    private fun gpxWaypointsToMarker(map: Map, wpt: TrackPoint, index: Int, defaultName: String): MarkerGson.Marker {
        val marker = wpt.toMarker(map)

        marker.name = if (wpt.name?.isNotEmpty() == true) {
            wpt.name
        } else {
            "$defaultName-wpt${index + 1}"
        }

        return marker
    }
}
