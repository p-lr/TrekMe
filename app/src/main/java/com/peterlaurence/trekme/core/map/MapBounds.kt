package com.peterlaurence.trekme.core.map

import kotlin.math.abs

/**
 * Holds the bounds coordinates of:
 *
 *  * The top-left corner of the map : (projectionX0, projectionY0) or (lon0, lat0) depending
 * on the map using a projection or not.
 *  * The bottom-right corner of the map : (projectionX1, projectionY1) or (lon1, lat1)
 *
 */
class MapBounds(var X0: Double, var Y0: Double, var X1: Double, var Y1: Double) {
    fun compareTo(x0: Double, y0: Double, x1: Double, y1: Double): Boolean {
        return doubleIsEqual(X0, x0, DELTA) && doubleIsEqual(Y0, y0, DELTA) &&
                doubleIsEqual(X1, x1, DELTA) && doubleIsEqual(Y1, y1, DELTA)
    }

    companion object {
        var DELTA = 0.0000001
        private fun doubleIsEqual(d1: Double, d2: Double, delta: Double): Boolean {
            return if (d1.compareTo(d2) == 0) {
                true
            } else abs(d1 - d2) <= delta
        }
    }
}