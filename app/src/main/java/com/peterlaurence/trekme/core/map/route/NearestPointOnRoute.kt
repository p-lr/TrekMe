package com.peterlaurence.trekme.core.map.route

import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.map.gson.RouteGson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sqrt


class NearestPointCalculator(val route: RouteGson.Route, val scope: CoroutineScope) {

    private val chunkSize = sqrt(route.route_markers.size / 3.0).toInt()

    init {
        scope.launch(Dispatchers.Default) {
            val chunker = Chunker(route.route_markers, chunkSize)
        }
    }

    fun findNearestPointOnRoute(lat: Double, lon: Double): MarkerGson.Marker {
        TODO()
    }
}

private class Chunker(val points: List<MarkerGson.Marker>, chunkSize: Int) {
    private val chunksByBarycenter: Map<Barycenter, List<MarkerGson.Marker>>
    private val barycenters: List<Barycenter>

    init {
        val chunks = points.chunked(chunkSize)
        chunksByBarycenter = chunks.associateBy {
            get2DBarycenter(it)
        }
        barycenters = chunksByBarycenter.keys.toList()
    }

    fun getBarycenters(): List<Barycenter> = barycenters

    fun getBarycenterGroup(barycenter: Barycenter): BarycenterGroup {
        val index = barycenters.indexOf(barycenter)
        val previous = if (index > 0) {
            barycenters[index - 1]
        } else null
        val next = if (index < barycenters.size - 1) {
            barycenters.lastOrNull()
        } else null

        return BarycenterGroup(barycenter, previous, next)
    }

    private fun get2DBarycenter(points: List<MarkerGson.Marker>): Barycenter {
        var sumLat = 0.0
        var sumLon = 0.0
        for (point in points) {
            sumLat += point.lat
            sumLon += point.lon
        }
        return Barycenter(sumLat / points.size, sumLon / points.size)
    }
}

private data class BarycenterGroup(val current: Barycenter, val previous: Barycenter?, val next: Barycenter?)
private data class Barycenter(val lat: Double, val lon: Double)
//private data class Barycenter(val point: Point, val index: Int)
