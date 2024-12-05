package com.peterlaurence.trekme.features.map.domain.core

import com.peterlaurence.trekme.core.map.domain.models.MapBounds
import com.peterlaurence.trekme.core.projection.Projection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility method to get latitude and longitude from normalized coordinates. This call is appropriate
 * for a one-time call from the main thread.
 *
 * @param x Normalized X position on the map
 * @param y Normalized Y position on the map
 * @param projection The projection used. If null, the result is the interpolation between the map
 *        bounds.
 *
 * @return An array of two elements: the longitude and the latitude
 */
suspend fun getLonLatFromNormalizedCoordinate(
    x: Double,
    y: Double,
    projection: Projection?,
    mapBounds: MapBounds
): DoubleArray = withContext(Dispatchers.Default) {
    getLonLatFromNormalizedCoordinateBlocking(x, y, projection, mapBounds)
}

/**
 * Utility method to get latitude and longitude from normalized coordinates. This call is typically
 * used off ui-thread for a list of normalized coordinates.
 *
 * @param x Normalized X position on the map
 * @param y Normalized Y position on the map
 * @param projection The projection used. If null, the result is the interpolation between the map
 *        bounds.
 *
 * @return An array of two elements: the longitude and the latitude
 */
fun getLonLatFromNormalizedCoordinateBlocking(
    x: Double,
    y: Double,
    projection: Projection?,
    mapBounds: MapBounds
): DoubleArray {
    val relativeX = deNormalize(x, mapBounds.X0, mapBounds.X1)
    val relativeY = deNormalize(y, mapBounds.Y0, mapBounds.Y1)

    val lonLat =
        projection?.undoProjection(relativeX, relativeY)
     ?: doubleArrayOf(relativeX, relativeY)

    return lonLat
}

suspend fun getNormalizedCoordinates(
    lat: Double,
    lon: Double,
    mapBounds: MapBounds,
    projection: Projection?
): DoubleArray {
    val projectedValues = withContext(Dispatchers.Default) {
        projection?.doProjection(lat, lon)
    } ?: doubleArrayOf(lon, lat)

    val x = normalize(projectedValues[0], mapBounds.X0, mapBounds.X1)
    val y = normalize(projectedValues[1], mapBounds.Y0, mapBounds.Y1)

    return doubleArrayOf(x, y)
}

private fun normalize(t: Double, min: Double, max: Double): Double {
    return (t - min) / (max - min)
}

private fun deNormalize(t: Double, min: Double, max: Double): Double {
    return min + t * (max - min)
}