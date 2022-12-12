package com.peterlaurence.trekme.features.map.domain.interactors

import com.peterlaurence.trekme.core.geotools.distanceApprox
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.features.map.domain.core.getNormalizedCoordinates
import com.peterlaurence.trekme.features.map.domain.models.NormalizedPos
import javax.inject.Inject

class MapInteractor @Inject constructor() {
    suspend fun getNormalizedCoordinates(
        map: Map,
        latitude: Double,
        longitude: Double
    ): NormalizedPos {
        return getNormalizedCoordinates(latitude, longitude, map.mapBounds, map.projection).let {
            NormalizedPos(it[0], it[1])
        }
    }

    fun getMapFullWidthDistance(map: Map): Double? {
        val projection = map.projection
        val b = map.mapBounds
        return if (projection != null) {
            val (lon1, lat1) = projection.undoProjection(b.X0, b.Y0) ?: return null
            val (lon2, lat2) = projection.undoProjection(b.X1, b.Y0) ?: return null
            distanceApprox(lat1, lon1, lat2, lon2)
        } else {
            distanceApprox(b.Y0, b.X0, b.Y0, b.X1)
        }
    }
}