package com.peterlaurence.trekme.core.map.route

import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.map.gson.RouteGson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt


class NearestMarkerCalculator(val route: RouteGson.Route, scope: CoroutineScope) {

    private val chunkSize = sqrt(route.route_markers.size / 3.0).toInt()
    private var chunker: Chunker? = null

    private val channel = Channel<Point>(Channel.CONFLATED)
    private val outputChannel = ConflatedBroadcastChannel<MarkerIndexed>()
    val nearestMarkersFlow = outputChannel.asFlow()

    init {
        scope.launch(Dispatchers.Default) {
            for (point in channel) {
                val marker = findNearestPointOnRoute(point.x, point.y)
                if (marker != null) outputChannel.offer(marker)
            }
        }
    }

    fun updatePosition(x: Double, y: Double) {
        channel.offer(Point(x, y)) // always returns true
    }

    private fun findNearestPointOnRoute(x: Double, y: Double): MarkerIndexed? {
        val chunker = getOrMakeChunker()
        val markers = chunker.getMarkersInVicinity(x, y)

        var distMin = Double.MAX_VALUE
        var nearestMarker: MarkerGson.Marker? = null
        for (markerList in markers) {
            for (marker in markerList) {
                val d = computeDistSquared(x, y, marker.proj_x, marker.proj_y)
                if (d < distMin) {
                    distMin = d
                    nearestMarker = marker
                }
            }
        }
        val index = route.route_markers.indexOf(nearestMarker)
        return if (nearestMarker != null) MarkerIndexed(nearestMarker, index) else null
    }

    private fun getOrMakeChunker(): Chunker {
        if (chunker != null) return chunker!!
        return Chunker(route.route_markers, chunkSize)
    }

    private fun computeDistSquared(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        return (x2 - x1).pow(2) + (y2 - y1).pow(2)
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

    fun getMarkersInVicinity(x: Double, y: Double): List<List<MarkerGson.Marker>> {
        val barycenter = getNearestBarycenter(x, y)
        val index = barycenters.indexOf(barycenter)
        println("Barycenter index : $index")
        val previous = if (index > 0) {
            barycenters[index - 1]
        } else null
        val next = if (index < barycenters.size - 1) {
            barycenters[index + 1]
        } else null

        return listOf(barycenter, previous, next).mapNotNull {
            it?.let { chunksByBarycenter[it] }
        }
    }

    private fun get2DBarycenter(points: List<MarkerGson.Marker>): Barycenter {
        var sumX = 0.0
        var sumY = 0.0
        for (point in points) {
            sumX += point.proj_x
            sumY += point.proj_y
        }
        return Barycenter(sumX / points.size, sumY / points.size)
    }

    private fun getNearestBarycenter(x: Double, y: Double): Barycenter {
        return barycenters.minBy {
            computeNorm(x, y, it)
        } ?: barycenters.first()
    }

    private fun computeNorm(x: Double, y: Double, barycenter: Barycenter): Double {
        return (barycenter.x - x).pow(2) + (barycenter.y - y).pow(2)
    }
}

private data class Barycenter(val x: Double, val y: Double)
private data class Point(val x: Double, val y: Double)

/**
 * A [MarkerGson.Marker] which also bundles the index of this marker on the corresponding route.
 */
data class MarkerIndexed(val marker: MarkerGson.Marker, val index: Int)
