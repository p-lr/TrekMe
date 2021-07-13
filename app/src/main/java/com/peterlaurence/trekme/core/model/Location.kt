package com.peterlaurence.trekme.core.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

/**
 * [latitude] and [longitude] are in decimal degrees.
 * [altitude] is in meters. Is null when this location doesn't have this information.
 * [speed] is in meters per second. Is null when this location doesn't have this information.
 * [time] is the UTC time in milliseconds since January 1, 1970
 */
data class Location(val latitude: Double = 0.0, val longitude: Double = 0.0, val speed: Float? = null,
                    val altitude: Double? = null, val time: Long = 0L)

/**
 * The [LocationSource] has two possible modes.
 * * [Mode.INTERNAL]: Uses the GPS antenna of the device, or
 * * [Mode.EXTERNAL]: Uses an external GPS
 *
 * Under the hood, the location source uses several [LocationProducer]s. For instance, it only uses
 * one producer at a give point in time. However, that may change in the future.
 *
 * The location source is intended to be application-wide. Components of the app get locations from
 * this source, and this source only.
 */
interface LocationSource {
    val locationFlow: SharedFlow<Location>

    enum class Mode {
        INTERNAL, EXTERNAL
    }
}

/**
 * A unique and indivisible location producer. For example: The device GPS antenna.
 */
interface LocationProducer {
    val locationFlow: Flow<Location>
}