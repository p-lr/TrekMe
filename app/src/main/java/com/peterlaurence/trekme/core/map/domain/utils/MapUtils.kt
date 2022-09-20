package com.peterlaurence.trekme.core.map.domain.utils

import com.peterlaurence.trekme.core.map.domain.models.Map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Create an empty ".nomedia" file at the root of the map directory. This way, other apps don't
 * index this content for media files.
 */
@Suppress("BlockingMethodInNonBlockingContext")
suspend fun createNomediaFile(directory: File) = withContext(Dispatchers.IO) {
    val noMedia = File(directory, ".nomedia")
    noMedia.createNewFile()
}

/**
 * Utility method to get latitude and longitude from normalized coordinates.
 *
 * @param x Normalized X position on the map
 * @param y Normalized Y position on the map
 *
 * @return An array of two elements: the longitude and the latitude
 */
fun getLonLat(x: Double, y: Double, map: Map): DoubleArray? {
    val bounds = map.mapBounds
    val projection = map.projection
    return if (projection != null) {
        val projX = bounds.X0 + (bounds.X0 - bounds.X1) * x
        val projY = bounds.Y0 + (bounds.Y0 - bounds.Y1) * y
        projection.undoProjection(projX, projY)
    } else {
        /* If no projection, the bounds are assumed to be in longitude / latitude */
        doubleArrayOf((bounds.X0 - bounds.X1) * x, (bounds.Y0 - bounds.Y1) * y)
    }
}