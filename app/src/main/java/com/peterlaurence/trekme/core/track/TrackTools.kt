package com.peterlaurence.trekme.core.track

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.map.gson.RouteGson
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
     * Add new [RouteGson.Route]s to a [Map].
     *
     * @return the number of [RouteGson.Route] that have been appended to the list.
     */
    fun updateRouteList(map: Map, newRouteList: List<RouteGson.Route>?): Int {
        if (newRouteList == null) return 0
        val hashMap = HashMap<String, RouteGson.Route>()
        val routeList = map.routes
        if (routeList != null) {
            for (route in routeList) {
                hashMap[route.name] = route
            }
        }

        var newRouteCount = 0
        for (route in newRouteList) {
            if (hashMap.containsKey(route.name)) {
                val existingRoute = hashMap[route.name]
                existingRoute?.copyRoute(route)
            } else {
                map.addRoute(route)
                newRouteCount++
            }
        }

        return newRouteCount
    }

    fun updateMarkerList(map: Map, newMarkerList: List<MarkerGson.Marker>): Int {
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
}