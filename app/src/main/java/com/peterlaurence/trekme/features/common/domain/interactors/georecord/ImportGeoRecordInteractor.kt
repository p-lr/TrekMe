package com.peterlaurence.trekme.features.common.domain.interactors.georecord

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.repositories.map.RouteRepository
import com.peterlaurence.trekme.core.lib.gpx.model.*
import com.peterlaurence.trekme.core.map.domain.dao.MarkersDao
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.core.track.TrackTools
import com.peterlaurence.trekme.features.common.domain.model.ElevationSource
import com.peterlaurence.trekme.features.common.domain.model.ElevationSourceInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import javax.inject.Inject

/**
 * Interactor to import a [GeoRecord] into a map.
 *
 * @since 03/03/17 -- converted to Kotlin on 16/09/18
 */
class ImportGeoRecordInteractor @Inject constructor(
    val routeRepository: RouteRepository,
    private val markersDao: MarkersDao,
    private val geoRecordParser: GeoRecordParser
) {
    /**
     * Applies the GPX content given as an [Uri] to the provided [Map].
     */
    suspend fun applyGpxUriToMap(
        uri: Uri, contentResolver: ContentResolver, map: Map
    ): GeoRecordImportResult {
        return runCatching {
            geoRecordParser.parse(uri, contentResolver)?.let { (uuid, routeGroups, markers) ->
                val routes = routeGroups.flatMap { it.routes }
                setRoutesAndMarkersToMap(map, routes, markers)
            } ?: GeoRecordImportResult.GeoRecordImportError
        }.onFailure {
            Log.e(TAG, "File with uri $uri doesn't exists")
        }.getOrNull() ?: GeoRecordImportResult.GeoRecordImportError
    }

    /**
     * Applies the GPX content given as a [File] to the provided [Map].
     */
    suspend fun applyGpxFileToMap(file: File, map: Map): GeoRecordImportResult {
        return try {
            val fileInputStream = FileInputStream(file)
            applyGpxInputStreamToMap(fileInputStream, map, file.name)
        } catch (e: Exception) {
            GeoRecordImportResult.GeoRecordImportError
        }
    }

    /**
     * Parses a [Gpx] from the given [InputStream]. Then, on the calling [CoroutineScope] (which
     * [CoroutineDispatcher] should be [Dispatchers.Main]), applies the result on the provided [Map].
     */
    private suspend fun applyGpxInputStreamToMap(
        input: InputStream,
        map: Map,
        defaultName: String
    ): GeoRecordImportResult {
        val pair = geoRecordParser.parse(input, defaultName)

        return if (pair != null) {
            val newRoutes = pair.routeGroups.flatMap { it.routes }
            return setRoutesAndMarkersToMap(map, newRoutes, pair.markers)
        } else {
            GeoRecordImportResult.GeoRecordImportError
        }
    }

    private suspend fun setRoutesAndMarkersToMap(
        map: Map,
        newRoutes: List<Route>,
        wayPoints: List<Marker>,
    ): GeoRecordImportResult {
        return try {
            /* Add the new routes and markers, and save the modifications */
            val newRouteCount = TrackTools.updateRouteList(map, newRoutes)
            val newMarkersCount = TrackTools.updateMarkerList(map, wayPoints)
            newRoutes.forEach {
                routeRepository.saveNewRoute(map, it)
            }
            markersDao.saveMarkers(map)
            GeoRecordImportResult.GeoRecordImportOk(map, newRoutes, wayPoints, newRouteCount, newMarkersCount)
        } catch (e: Exception) {
            GeoRecordImportResult.GeoRecordImportError
        }
    }
}

fun GeoRecord.hasTrustedElevations() : Boolean {
    return elevationSourceInfo.hasTrustedElevations()
}

fun ElevationSourceInfo?.hasTrustedElevations() : Boolean {
    return this?.elevationSource == ElevationSource.IGN_RGE_ALTI
}

fun GeoRecord.getElevationSource(): ElevationSource {
    return elevationSourceInfo?.elevationSource ?: ElevationSource.UNKNOWN
}

private const val TAG = "TrackImporter"
