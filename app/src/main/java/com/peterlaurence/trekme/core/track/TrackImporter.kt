package com.peterlaurence.trekme.core.track

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.peterlaurence.trekme.core.georecord.data.convertGpx
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.repositories.map.RouteRepository
import com.peterlaurence.trekme.core.lib.gpx.model.*
import com.peterlaurence.trekme.core.map.domain.dao.MarkersDao
import com.peterlaurence.trekme.core.georecord.domain.interactors.GeoRecordParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.inject.Inject

/**
 * Interactor for gpx import.
 * TODO: The app is missing a domain type for geographic data (for instance, the corresponding data type
 * is [Gpx]). When, for example, "GeoRecord" replaces [Gpx] usage at domain level, this class should
 * be renamed "GeoRecordImporter".
 *
 * @since 03/03/17 -- converted to Kotlin on 16/09/18
 */
class TrackImporter @Inject constructor(
    val routeRepository: RouteRepository,
    private val markersDao: MarkersDao,
    private val geoRecordParser: GeoRecordParser
) {
    /**
     * Applies the GPX content given as an [Uri] to the provided [Map].
     */
    suspend fun applyGpxUriToMap(
        uri: Uri, contentResolver: ContentResolver, map: Map
    ): GpxImportResult {
        return runCatching {
            geoRecordParser.parse(uri, contentResolver)?.let { (routes, markers) ->
                setRoutesAndMarkersToMap(map, routes, markers)
            } ?: GpxImportResult.GpxImportError
        }.onFailure {
            Log.e(TAG, "File with uri $uri doesn't exists")
        }.getOrNull() ?: GpxImportResult.GpxImportError
    }

    /**
     * Applies the GPX content given as a [File] to the provided [Map].
     */
    suspend fun applyGpxFileToMap(file: File, map: Map): GpxImportResult {
        return try {
            val fileInputStream = FileInputStream(file)
            applyGpxInputStreamToMap(fileInputStream, map, file.name)
        } catch (e: Exception) {
            GpxImportResult.GpxImportError
        }
    }

    /**
     * Applies the GPX content given directly as a [Gpx] instance to the provided [Map].
     * TODO: The [Gpx] type is a data type a shouldn't be used here. Instead, it should have been
     * converted to domain type beforehand and passed here.
     */
    suspend fun applyGpxToMap(gpx: Gpx, map: Map): GpxImportResult {
        val data = convertGpx(gpx)
        return setRoutesAndMarkersToMap(map, data.routes, data.markers)
    }

    sealed class GpxImportResult {
        data class GpxImportOk(
            val map: Map, val routes: List<Route>, val wayPoints: List<Marker>,
            val newRouteCount: Int, val newMarkersCount: Int
        ) : GpxImportResult()

        object GpxImportError : GpxImportResult()
    }

    /**
     * Parses a [Gpx] from the given [InputStream]. Then, on the calling [CoroutineScope] (which
     * [CoroutineDispatcher] should be [Dispatchers.Main]), applies the result on the provided [Map].
     */
    private suspend fun applyGpxInputStreamToMap(
        input: InputStream,
        map: Map,
        defaultName: String
    ): GpxImportResult {
        val pair = geoRecordParser.parse(input, defaultName)

        return if (pair != null) {
            return setRoutesAndMarkersToMap(map, pair.routes, pair.markers)
        } else {
            GpxImportResult.GpxImportError
        }
    }

    private suspend fun setRoutesAndMarkersToMap(
        map: Map, newRoutes: List<Route>,
        wayPoints: List<Marker>,
    ): GpxImportResult {
        return try {
            /* Add the new routes and markers, and save the modifications */
            val newRouteCount = TrackTools.updateRouteList(map, newRoutes)
            val newMarkersCount = TrackTools.updateMarkerList(map, wayPoints)
            newRoutes.forEach {
                routeRepository.saveNewRoute(map, it)
            }
            markersDao.saveMarkers(map)
            GpxImportResult.GpxImportOk(map, newRoutes, wayPoints, newRouteCount, newMarkersCount)
        } catch (e: Exception) {
            GpxImportResult.GpxImportError
        }
    }
}

private const val TAG = "TrackImporter"
