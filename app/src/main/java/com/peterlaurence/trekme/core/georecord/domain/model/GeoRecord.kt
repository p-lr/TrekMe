package com.peterlaurence.trekme.core.georecord.domain.model

import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.features.common.domain.model.ElevationSource
import com.peterlaurence.trekme.features.common.domain.model.ElevationSourceInfo
import java.util.*

/**
 * The domain representation of a recording. An actual recording can be e.g a gpx file.
 *
 * [time] is the UTC time in milliseconds since January 1, 1970
 */
data class GeoRecord(
    val id: UUID,
    val routeGroups: List<RouteGroup>,
    val markers: List<Marker>,
    val time: Long?,
    val elevationSourceInfo: ElevationSourceInfo?,
    val name: String
)

/**
 * A regroup of several [Route].
 */
data class RouteGroup(val id: String, val routes: List<Route>, val name: String = "")

fun GeoRecord.hasTrustedElevations() : Boolean {
    return elevationSourceInfo.hasTrustedElevations()
}

fun ElevationSourceInfo?.hasTrustedElevations() : Boolean {
    return this?.elevationSource == ElevationSource.IGN_RGE_ALTI
}

fun GeoRecord.getElevationSource(): ElevationSource {
    return elevationSourceInfo?.elevationSource ?: ElevationSource.UNKNOWN
}

fun GeoRecord.getBoundingBox(): BoundingBox? {
    var minLat: Double? = null
    var maxLat: Double? = null
    var minLon: Double? = null
    var maxLon: Double? = null

    fun Double?.coerceMin(value: Double): Double {
        return this?.coerceAtMost(value) ?: value
    }

    fun Double?.coerceMax(value: Double): Double {
        return this?.coerceAtLeast(value) ?: value
    }

    routeGroups.forEach { group ->
        group.routes.forEach { route ->
            route.routeMarkers.forEach { marker ->
                minLat = minLat.coerceMin(marker.lat)
                maxLat = maxLat.coerceMax(marker.lat)
                minLon = minLon.coerceMin(marker.lon)
                maxLon = maxLon.coerceMax(marker.lon)
            }
        }
    }

    return if (minLat != null) {
        BoundingBox(minLat = minLat!!, maxLat = maxLat!!, minLon = minLon!!, maxLon = maxLon!!)
    } else null
}
