package com.peterlaurence.trekme.util.gpx.model

/**
 * Represents a waypoint, point of interest, or named feature on a map.
 *
 * @author P.Laurence on 12/02/17.
 */
data class TrackPoint(
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var elevation: Double? = null,
    /**
     * The UTC time of this point, in milliseconds since January 1, 1970.
     */
    var time: Long? = null,
    var name: String? = null
)