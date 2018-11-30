package com.peterlaurence.trekme.core.track

import android.content.ContentResolver
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import com.peterlaurence.trekme.core.TrekAdvisorContext.recordingsDir
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.ui.mapview.events.TrackChangedEvent
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.gpx.GPXParser
import com.peterlaurence.trekme.util.gpx.GPXWriter
import com.peterlaurence.trekme.util.gpx.model.Gpx
import com.peterlaurence.trekme.util.gpx.model.Track
import com.peterlaurence.trekme.util.gpx.model.TrackPoint
import com.peterlaurence.trekme.util.gpx.model.TrackSegment
import kotlinx.coroutines.Runnable
import org.greenrobot.eventbus.EventBus
import java.io.*
import java.util.*

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
     * [com.peterlaurence.trekme.core.TrekAdvisorContext.recordingsDir].
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
     * Parse a [File] that contains routes, and is in one of the supported formats. <br></br>
     * The parsing is done in an asynctask.
     *
     * @param uri      the track as an [Uri]
     * @param listener a [TrackFileParsedListener]
     * @param map      the [Map] to which the routes will be added.
     */
    fun importTrackUri(uri: Uri, listener: TrackFileParsedListener, map: Map,
                       contentResolver: ContentResolver) {

        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            if (parcelFileDescriptor == null) {
                listener.onError("Could not read content of file")
                return
            }
            val fileDescriptor = parcelFileDescriptor.fileDescriptor
            val fileInputStream = FileInputStream(fileDescriptor)
            val fileName = FileUtils.getFileRealPathFromURI(contentResolver, uri) ?: "A track"

            val gpxTrackFileToRoutesTask = GpxTrackFileToRoutesTask(listener, map, fileName, Runnable {
                try {
                    parcelFileDescriptor.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            })
            gpxTrackFileToRoutesTask.execute(fileInputStream)
        } catch (e: IOException) {
            listener.onError("Error when opening the file")
        }

    }

    fun importTrackFile(file: File, listener: TrackFileParsedListener?, map: Map) {
        try {
            val fileInputStream = FileInputStream(file)

            val gpxTrackFileToRoutesTask = GpxTrackFileToRoutesTask(listener, map, file.name, null)
            gpxTrackFileToRoutesTask.execute(fileInputStream)
        } catch (e: FileNotFoundException) {
            listener?.onError("The file doesn't exists")
        }

    }

    interface TrackFileParsedListener {
        fun onTrackFileParsed(map: Map, routeList: List<@JvmSuppressWildcards RouteGson.Route>, wayPoints: List<MarkerGson.Marker>, newRouteCount: Int, addedMarkers: Int)

        fun onError(message: String)
    }

    private class GpxTrackFileToRoutesTask internal constructor(private val mListener: TrackFileParsedListener?,
                                                                private val mMap: Map,
                                                                private val defaultName: String,
                                                                private val mPostExecuteTask: Runnable?) : AsyncTask<InputStream, Void, Void?>() {
        private val newRouteList: LinkedList<RouteGson.Route> = LinkedList()
        private val newWaypointList: LinkedList<MarkerGson.Marker> = LinkedList()
        private var isSuccess = true
        private var failReason = ""

        /**
         * Each gpx file may contain several tracks. And each [Track] may contain several
         * [TrackSegment]. <br></br>
         * A [Track] is the equivalent of a [RouteGson.Route], so all [TrackSegment]
         * are added to a single [RouteGson.Route].
         */
        override fun doInBackground(vararg inputStreamList: InputStream): Void? {
            for (stream in inputStreamList) {

                try {
                    val gpx = GPXParser.parse(stream)

                    gpx.tracks.mapIndexed { index, track ->
                        val route = gpxTracktoRoute(track, index)
                        newRouteList.add(route)
                    }

                    gpx.wayPoints.mapIndexed { index, wpt ->
                        val marker = gpxWaypointsToMarker(wpt, index)
                        newWaypointList.add(marker)
                    }

                    stream.close()
                } catch (e: Exception) {
                    val sw = StringWriter()
                    val pw = PrintWriter(sw)
                    e.printStackTrace(pw)
                    isSuccess = false
                    failReason = sw.toString()
                }
            }
            return null
        }

        override fun onPostExecute(result: Void?) {
            if (isSuccess) {
                val newRouteCount = TrackTools.updateRouteList(mMap, newRouteList)
                val addedMarkers = TrackTools.updateMarkerList(mMap, newWaypointList)
                MapLoader.getInstance().saveRoutes(mMap)
                MapLoader.getInstance().saveMarkers(mMap)

                mListener?.onTrackFileParsed(mMap, newRouteList, newWaypointList, newRouteCount, addedMarkers)

                EventBus.getDefault().post(TrackChangedEvent(mMap, newRouteList, addedMarkers))
                mPostExecuteTask?.run()
            } else {
                mListener?.onError(failReason)
            }
        }

        /**
         * Converts a [Track] into a [RouteGson.Route]. <br></br>
         * A single [Track] may contain several [TrackSegment].
         */
        private fun gpxTracktoRoute(track: Track, index: Int): RouteGson.Route {
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
                    val projection = mMap.projection
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

        private fun gpxWaypointsToMarker(wpt: TrackPoint, index: Int): MarkerGson.Marker {
            val marker = MarkerGson.Marker()

            marker.name = if (wpt.name?.isNotEmpty() == true) {
                wpt.name
            } else {
                "$defaultName-wpt${index + 1}"
            }

            /* If the map uses a projection, store projected values */
            val projectedValues: DoubleArray?
            val projection = mMap.projection
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
}
