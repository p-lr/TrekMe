package com.peterlaurence.trekme.core.georecord.domain.model

import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route
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
