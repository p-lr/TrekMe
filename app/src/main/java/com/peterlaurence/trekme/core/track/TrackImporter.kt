package com.peterlaurence.trekme.core.track

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Route
import com.peterlaurence.trekme.core.map.domain.Marker
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.repositories.map.RouteRepository
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.core.lib.gpx.model.*
import com.peterlaurence.trekme.core.lib.gpx.parseGpxSafely
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.inject.Inject

/**
 * Utility toolbox to :
 *
 *  * Import a gpx track file into a [Map].
 *  * Get the list of gpx files created by location recording.
 *  * Get the content of each gpx file as [Gpx] instances.
 *
 * @author P.Laurence on 03/03/17 -- converted to Kotlin on 16/09/18
 */
class TrackImporter @Inject constructor(
    val routeRepository: RouteRepository
) {
    /**
     * Applies the GPX content given as an [Uri] to the provided [Map].
     */
    suspend fun applyGpxUriToMap(
        uri: Uri, contentResolver: ContentResolver, map: Map, mapLoader: MapLoader
    ): GpxImportResult {
        return runCatching {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
            parcelFileDescriptor?.use {
                val fileDescriptor = parcelFileDescriptor.fileDescriptor
                FileInputStream(fileDescriptor).use { fileInputStream ->
                    val fileName = FileUtils.getFileRealFileNameFromURI(contentResolver, uri)
                        ?: "A track"
                    applyGpxInputStreamToMap(fileInputStream, map, fileName, mapLoader)
                }
            }
        }.onFailure {
            Log.e(TAG, "File with uri $uri doesn't exists")
        }.getOrNull() ?: GpxImportResult.GpxImportError
    }

    /**
     * Applies the GPX content given as a [File] to the provided [Map].
     */
    suspend fun applyGpxFileToMap(file: File, map: Map, mapLoader: MapLoader): GpxImportResult {
        return try {
            val fileInputStream = FileInputStream(file)
            applyGpxInputStreamToMap(fileInputStream, map, file.name, mapLoader)
        } catch (e: Exception) {
            GpxImportResult.GpxImportError
        }
    }

    /**
     * Applies the GPX content given directly as a [Gpx] instance to the provided [Map].
     */
    suspend fun applyGpxToMap(gpx: Gpx, map: Map, mapLoader: MapLoader): GpxImportResult {
        val data = convertGpx(gpx)
        return setRoutesAndMarkersToMap(map, data.first, data.second, mapLoader)
    }

    sealed class GpxImportResult {
        data class GpxImportOk(
            val map: Map, val routes: List<Route>, val wayPoints: List<Marker>,
            val newRouteCount: Int, val newMarkersCount: Int
        ) : GpxImportResult()

        object GpxImportError : GpxImportResult()
    }

    /**
     * Parses the GPX content provided as [InputStream], off UI thread.
     */
    private suspend fun readGpxInputStream(input: InputStream, map: Map, defaultName: String) =
        withContext(Dispatchers.Default) {
            parseGpxSafely(input)?.let { gpx ->
                convertGpx(gpx, defaultName)
            }
        }

    /**
     * Converts a [Gpx] instance into view-specific types.
     */
    private suspend fun convertGpx(
        gpx: Gpx,
        defaultName: String = "track"
    ): Pair<List<Route>, List<Marker>> = withContext(Dispatchers.Default) {
        val routes = gpx.tracks.mapIndexed { index, track ->
            gpxTrackToRoute(track, gpx.hasTrustedElevations(), index, defaultName)
        }.flatten()
        val waypoints = gpx.wayPoints.mapIndexed { index, wpt ->
            gpxWaypointsToMarker(wpt, index, defaultName)
        }
        Pair(routes, waypoints)
    }

    /**
     * Parses a [Gpx] from the given [InputStream]. Then, on the calling [CoroutineScope] (which
     * [CoroutineDispatcher] should be [Dispatchers.Main]), applies the result on the provided [Map].
     */
    private suspend fun applyGpxInputStreamToMap(
        input: InputStream, map: Map,
        defaultName: String,
        mapLoader: MapLoader
    ): GpxImportResult {
        val pair = readGpxInputStream(input, map, defaultName)

        return if (pair != null) {
            return setRoutesAndMarkersToMap(map, pair.first, pair.second, mapLoader)
        } else {
            GpxImportResult.GpxImportError
        }
    }

    private suspend fun setRoutesAndMarkersToMap(
        map: Map, newRoutes: List<Route>,
        wayPoints: List<Marker>,
        mapLoader: MapLoader
    ): GpxImportResult {
        return try {
            /* Add the new routes and markers, and save the modifications */
            val newRouteCount = TrackTools.updateRouteList(map, newRoutes)
            val newMarkersCount = TrackTools.updateMarkerList(map, wayPoints)
            newRoutes.forEach {
                routeRepository.saveNewRoute(map, it)
            }
            mapLoader.saveMarkers(map)
            GpxImportResult.GpxImportOk(map, newRoutes, wayPoints, newRouteCount, newMarkersCount)
        } catch (e: Exception) {
            GpxImportResult.GpxImportError
        }
    }

    /**
     * Converts a [Track] into a list of [Route] (a single [Track] may contain several [TrackSegment]).
     * This should be invoked off UI thread.
     */
    private fun gpxTrackToRoute(
        track: Track,
        elevationTrusted: Boolean,
        index: Int,
        defaultName: String
    ): List<Route> {

        /* The route name is the track name if it has one. Otherwise we take the default name */
        val name = if (track.name.isNotEmpty()) {
            track.name
        } else {
            "$defaultName#$index"
        }

        /* If there's more than one segment, the route name/id is the track name/id suffixed
         * with the segment index. */
        fun String?.formatNameOrId(i: Int): String? = if (this != null && track.trackSegments.size > 1) {
            this + "_$i"
        } else this

        /* Make a route for each track segment */
        return track.trackSegments.mapIndexed { i, segment ->
            val markers = segment.trackPoints.map { trackPoint ->
                trackPoint.toMarker()
            }.toMutableList()

            Route(
                id = segment.id,
                name = name.formatNameOrId(i),
                initialMarkers = markers,
                initialVisibility = true, /* The route should be visible by default */
                elevationTrusted = elevationTrusted
            )
        }
    }

    private fun gpxWaypointsToMarker(
        wpt: TrackPoint,
        index: Int,
        defaultName: String
    ): Marker {
        return wpt.toMarker().apply {
            name = if (wpt.name?.isNotEmpty() == true) {
                wpt.name ?: ""
            } else {
                "$defaultName-wpt${index + 1}"
            }
        }
    }
}

fun TrackPoint.toMarker(): Marker = Marker(lat = latitude, lon = longitude, elevation = elevation)


private const val TAG = "TrackImporter"
