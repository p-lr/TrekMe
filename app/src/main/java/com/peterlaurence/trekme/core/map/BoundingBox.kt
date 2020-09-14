package com.peterlaurence.trekme.core.map

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * All values are in decimal degrees.
 */
data class BoundingBox(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double)

fun BoundingBox.intersects(box: BoundingBox): Boolean {
    val inclusionLat = (box.minLat in minLat..maxLat && box.maxLat in minLat..maxLat) ||
            (minLat in box.minLat..box.maxLat && maxLat in box.minLat..box.maxLat)
    val inclusionLon = (box.minLon in minLon..maxLon && box.maxLon in minLon..maxLon) ||
            (minLon in box.minLon..box.maxLon && maxLat in box.minLon..box.maxLon)
    val intersectLat = box.minLat in minLat..maxLat || box.maxLat in minLat..maxLat
    val intersectLon = box.minLon in minLon..maxLon || box.maxLon in minLon..maxLon

    return (intersectLat || inclusionLat) && (intersectLon || inclusionLon)
}

fun BoundingBox.contains(latitude: Double, longitude: Double): Boolean {
    return latitude in minLat..maxLat && longitude in minLon..maxLon
}

suspend fun Map.intersects(box: BoundingBox): Boolean? {
    if (calibrationStatus != Map.CalibrationStatus.OK) return null
    val mapBounds = mapBounds ?: return null
    return withContext(Dispatchers.Default) {
        projection?.let { p ->
            val topLeft = p.undoProjection(mapBounds.X0, mapBounds.Y0) ?: return@withContext null
            val bottomRight = p.undoProjection(mapBounds.X1, mapBounds.Y1)
                    ?: return@withContext null
            BoundingBox(bottomRight[1], topLeft[1], topLeft[0], bottomRight[0]).intersects(box)
        } ?: BoundingBox(mapBounds.Y1, mapBounds.Y0, mapBounds.X1, mapBounds.X0).intersects(box)
    }
}

