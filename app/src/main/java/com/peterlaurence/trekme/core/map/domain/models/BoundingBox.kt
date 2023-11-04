package com.peterlaurence.trekme.core.map.domain.models

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * All values are in decimal degrees.
 */
data class BoundingBox(val minLat: Double, val maxLat: Double, val minLon: Double, val maxLon: Double)

data class BoundingBoxNormalized(val xLeft: Double, val yBottom: Double, val xRight: Double, val yTop: Double)

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

fun BoundingBox.contains(bb: BoundingBox): Boolean {
    return minLat <= bb.minLat && maxLat >= bb.maxLat && minLon <= bb.minLon && maxLon >= bb.maxLon
}

suspend fun Map.getBoundingBox(): BoundingBox? {
    if (calibrationStatus != CalibrationStatus.OK) return null
    val mapBounds = mapBounds
    return withContext(Dispatchers.Default) {
        projection?.let { p ->
            val topLeft = p.undoProjection(mapBounds.X0, mapBounds.Y0) ?: return@withContext null
            val bottomRight = p.undoProjection(mapBounds.X1, mapBounds.Y1)
                ?: return@withContext null
            BoundingBox(bottomRight[1], topLeft[1], topLeft[0], bottomRight[0])
        } ?: BoundingBox(mapBounds.Y1, mapBounds.Y0, mapBounds.X1, mapBounds.X0)
    }
}

suspend fun Map.intersects(box: BoundingBox): Boolean {
    return getBoundingBox()?.intersects(box) ?: false
}

suspend fun Map.contains(box: BoundingBox): Boolean {
    return getBoundingBox()?.contains(box) ?: false
}

