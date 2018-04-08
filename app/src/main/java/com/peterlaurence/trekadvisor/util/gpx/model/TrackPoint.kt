package com.peterlaurence.trekadvisor.util.gpx.model

import java.util.Date

/**
 * Represents a waypoint, point of interest, or named feature on a map.
 *
 * @author peterLaurence on 12/02/17.
 */
class TrackPoint(
        val latitude: Double,
        val longitude: Double,
        val elevation: Double?,
        val time: Date?
) {
    private constructor(builder: Builder) : this(builder.latitude, builder.longitude,
            builder.elevation, builder.time)

    class Builder {
        var latitude: Double = 0.0
            private set
        var longitude: Double = 0.0
            private set
        var elevation: Double? = null
            private set
        var time: Date? = null
            private set

        fun setLatitude(latitude: Double) = apply { this.latitude = latitude }

        fun setLongitude(longitude: Double) = apply { this.longitude = longitude }

        fun setElevation(elevation: Double?) = apply { this.elevation = elevation }

        fun setTime(time: Date) = apply { this.time = time }

        fun build() = TrackPoint(this)
    }
}
