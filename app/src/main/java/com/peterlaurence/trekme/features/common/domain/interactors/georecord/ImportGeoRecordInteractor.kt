package com.peterlaurence.trekme.features.common.domain.interactors.georecord

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.repository.RouteRepository
import com.peterlaurence.trekme.core.map.domain.dao.MarkersDao
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.features.common.domain.model.GeoRecordImportResult
import com.peterlaurence.trekme.features.common.domain.model.ElevationSource
import com.peterlaurence.trekme.features.common.domain.model.ElevationSourceInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.HashMap
import javax.inject.Inject

/**
 * Interactor to import a [GeoRecord] into a map.
 * A [GeoRecord] is the domain representation of a recording. It can correspond to a gpx file, or
 * other format which may be supported in the future.
 *
 * @since 03/03/17 -- converted to Kotlin on 16/09/18
 */
class ImportGeoRecordInteractor @Inject constructor(
    val routeRepository: RouteRepository,
    private val markersDao: MarkersDao,
    private val geoRecordParser: GeoRecordParser
) {
    /**
     * Applies the geo record content given as an [Uri] to the provided [Map].
     */
    suspend fun applyGeoRecordUriToMap(
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
     * Applies the geo record content given as a [File] to the provided [Map].
     */
    suspend fun applyGeoRecordFileToMap(file: File, map: Map): GeoRecordImportResult {
        return try {
            val fileInputStream = FileInputStream(file)
            applyGeoRecordInputStreamToMap(fileInputStream, map, file.name)
        } catch (e: Exception) {
            GeoRecordImportResult.GeoRecordImportError
        }
    }

    /**
     * Parses a geo record from the given [InputStream]. Then, on the calling [CoroutineScope] (which
     * [CoroutineDispatcher] should be [Dispatchers.Main]), applies the result on the provided [Map].
     */
    private suspend fun applyGeoRecordInputStreamToMap(
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
            val newRouteCount = updateRouteList(map, newRoutes)
            val newMarkersCount = updateMarkerList(map, wayPoints)
            newRoutes.forEach {
                routeRepository.saveNewRoute(map, it)
            }
            markersDao.saveMarkers(map)
            GeoRecordImportResult.GeoRecordImportOk(map, newRoutes, wayPoints, newRouteCount, newMarkersCount)
        } catch (e: Exception) {
            GeoRecordImportResult.GeoRecordImportError
        }
    }

    private fun updateRouteList(map: Map, newRouteList: List<Route>?): Int {
        if (newRouteList == null) return 0
        val hashMap = HashMap<String, Route>()
        val routeList = map.routes.value
        for (route in routeList) {
            hashMap[route.id] = route
        }

        var newRouteCount = 0
        for (route in newRouteList) {
            if (hashMap.containsKey(route.id)) {
                hashMap[route.id]?.also { existing ->
                    map.routes.update {
                        it.map { r ->
                            if (r == existing) route else r
                        }
                    }
                }
            } else {
                map.routes.update { it + route }
                newRouteCount++
            }
        }

        return newRouteCount
    }

    private fun updateMarkerList(map: Map, newMarkerList: List<Marker>): Int {
        val toBeAdded = newMarkerList.toMutableList()
        val existing = map.markers.value
        existing.also {
            toBeAdded.removeAll(existing)
        }
        map.markers.update { it + toBeAdded }
        return toBeAdded.count()
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
