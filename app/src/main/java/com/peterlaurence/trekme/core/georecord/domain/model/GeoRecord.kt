package com.peterlaurence.trekme.core.georecord.domain.model

import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route

/**
 * The domain representation of a recording. For the moment, the only supported recording format is
 * gpx.
 */
data class GeoRecord(val routes: List<Route>, val markers: List<Marker>)
