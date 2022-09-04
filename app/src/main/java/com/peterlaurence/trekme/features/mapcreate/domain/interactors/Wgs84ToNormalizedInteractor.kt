package com.peterlaurence.trekme.features.mapcreate.domain.interactors

import com.peterlaurence.trekme.core.mapsource.wmts.X0
import com.peterlaurence.trekme.core.mapsource.wmts.X1
import com.peterlaurence.trekme.core.mapsource.wmts.Y0
import com.peterlaurence.trekme.core.mapsource.wmts.Y1
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.features.mapcreate.domain.model.NormalizedPos
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Convert lat/lon to normalized coordinates.
 * In the context of the map creation, all providers use the same [MercatorProjection].
 */
@Singleton
class Wgs84ToNormalizedInteractor @Inject constructor() {
    private val projection = MercatorProjection()

    /**
     * This is a *blocking* call.
     */
    fun getNormalized(lat: Double, lon: Double): NormalizedPos? {
        return projection.doProjection(lat, lon)?.let {
            NormalizedPos(normalize(it[0], X0, X1), normalize(it[1], Y0, Y1))
        }
    }

    private fun normalize(t: Double, min: Double, max: Double): Double {
        return (t - min) / (max - min)
    }
}