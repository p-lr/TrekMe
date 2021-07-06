package com.peterlaurence.trekme.core.model

/**
 * [latitude] and [longitude] are in decimal degrees.
 * [altitude] is in meters. Is null when this location doesn't have this information.
 * [speed] is in meters per second. Is null when this location doesn't have this information.
 * [time] is the UTC time in milliseconds since January 1, 1970
 */
data class Location(val latitude: Double = 0.0, val longitude: Double = 0.0, val speed: Float? = null,
                    val altitude: Double? = null, val time: Long = 0L)