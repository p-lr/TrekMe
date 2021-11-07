package com.peterlaurence.trekme.core.track

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Route
import com.peterlaurence.trekme.core.map.domain.Marker
import com.peterlaurence.trekme.util.gpx.model.Gpx
import com.peterlaurence.trekme.util.gpx.model.Track
import com.peterlaurence.trekme.util.gpx.model.hasTrustedElevations
import java.io.File
import java.util.*

object TrackTools {
    /**
     * Rename a GPX file.
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
        val routeList = map.routes
        if (routeList != null) {
            for (route in routeList) {
                hashMap[route.compositeId] = route
            }
        }

        var newRouteCount = 0
        for (route in newRouteList) {
            if (hashMap.containsKey(route.compositeId)) {
                hashMap[route.compositeId]?.also { existing ->
                    map.replaceRoute(existing, route)
                }
            } else {
                map.addRoute(route)
                newRouteCount++
            }
        }

        return newRouteCount
    }

    fun updateMarkerList(map: Map, newMarkerList: List<Marker>): Int {
        val toBeAdded = newMarkerList.toMutableList()
        val existing = map.markers
        existing?.let {
            toBeAdded.removeAll(existing)
        }
        toBeAdded.forEach {
            map.addMarker(it)
        }
        return toBeAdded.count()
    }

    fun getTrackStatistics(track: Track, gpx: Gpx): TrackStatistics {
        val statCalculatorList = track.trackSegments.map { trackSegment ->
            val distanceCalculator = DistanceCalculatorImpl(gpx.hasTrustedElevations())
            val statCalculator = TrackStatCalculator(distanceCalculator)
            statCalculator.addTrackPointList(trackSegment.trackPoints)
            statCalculator
        }

        return statCalculatorList.mergeStats()
    }
}