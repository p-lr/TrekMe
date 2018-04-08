package com.peterlaurence.trekadvisor.util.gpx.model

import java.util.Date

/**
 * Represents a waypoint, point of interest, or named feature on a map.
 *
 * @author peterLaurence on 12/02/17.
 */
class TrackPoint(
        var latitude: Double = 0.0,
        var longitude: Double = 0.0,
        var elevation: Double? = null,
        var time: Date? = null
)