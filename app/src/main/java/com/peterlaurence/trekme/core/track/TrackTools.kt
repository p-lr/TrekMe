package com.peterlaurence.trekme.core.track

import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.features.common.domain.interactors.georecord.hasTrustedElevations
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.*

object TrackTools {
    /**
     * Rename a GPX file.
     * TODO: move this function to data layer
     */
    fun renameGpxFile(gpxFile: File, newFile: File): Boolean {
        return try {
            gpxFile.renameTo(newFile)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Add new [Route]s to a [Map].
     *
     * @return the number of [Route] that have been appended to the list.
     */
    fun updateRouteList(map: Map, newRouteList: List<Route>?): Int {
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
                    map.replaceRoute(existing, route)
                }
            } else {
                map.addRoute(route)
                newRouteCount++
            }
        }

        return newRouteCount
    }

    /**
     * TODO: this should be a method "addMarkers" in an interactor
     */
    fun updateMarkerList(map: Map, newMarkerList: List<Marker>): Int {
        val toBeAdded = newMarkerList.toMutableList()
        val existing = map.markers.value
        existing.also {
            toBeAdded.removeAll(existing)
        }
        map.markers.update { it + toBeAdded }
        return toBeAdded.count()
    }

    fun getGeoStatistics(geoRecord: GeoRecord): GeoStatistics {
        val statCalculatorList = geoRecord.routeGroups.flatMap { it.routes }.map { route ->
            val distanceCalculator = distanceCalculatorFactory(geoRecord.hasTrustedElevations())
            val statCalculator = TrackStatCalculator(distanceCalculator)
            route.routeMarkers.forEach {
                statCalculator.addTrackPoint(it.lat, it.lon, it.elevation, it.time)
            }
            statCalculator
        }

        return statCalculatorList.mergeStats()
    }
}