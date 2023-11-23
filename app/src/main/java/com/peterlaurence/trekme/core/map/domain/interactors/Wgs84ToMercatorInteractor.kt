package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.wmts.domain.model.X0
import com.peterlaurence.trekme.core.wmts.domain.model.X1
import com.peterlaurence.trekme.core.wmts.domain.model.Y0
import com.peterlaurence.trekme.core.wmts.domain.model.Y1
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.wmts.domain.model.Point
import com.peterlaurence.trekme.features.map.domain.models.NormalizedPos
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Convert lat/lon to normalized coordinates in mercator projection.
 * In the context of the map creation, all providers use the same [MercatorProjection].
 */
@Singleton
class Wgs84ToMercatorInteractor @Inject constructor() {
    private val projection = MercatorProjection()

    /**
     * Shouldn't be invoked repeatedly on ui-thread.
     */
    fun getNormalized(lat: Double, lon: Double): NormalizedPos? {
        return projection.doProjection(lat, lon)?.let {
            NormalizedPos(normalize(it[0], X0, X1), normalize(it[1], Y0, Y1))
        }
    }

    /**
     * Shouldn't be invoked repeatedly on ui-thread.
     */
    fun getProjected(lat: Double, lon: Double): Point? {
        return projection.doProjection(lat, lon)?.let {
            Point(it[0], it[1])
        }
    }

    /**
     * Shouldn't be invoked repeatedly on ui-thread.
     */
    fun getLatLonFromNormalized(x: Double, y: Double): DoubleArray? {
        val X = deNormalize(x, X0, X1)
        val Y = deNormalize(y, Y0, Y1)
        return projection.undoProjection(X, Y)
    }

    private fun normalize(t: Double, min: Double, max: Double): Double {
        return (t - min) / (max - min)
    }

    private fun deNormalize(t: Double, min: Double, max: Double): Double {
        return min + t * (max - min)
    }
}