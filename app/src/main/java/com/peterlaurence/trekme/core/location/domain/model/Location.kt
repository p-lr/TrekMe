package com.peterlaurence.trekme.core.location.domain.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.Serializable
import kotlin.time.TimeSource

/**
 * [latitude] and [longitude] are in decimal degrees.
 * [altitude] is in meters. Is null when this location doesn't have this information.
 * [speed] is in meters per second. Is null when this location doesn't have this information.
 * [time] is the UTC time in milliseconds since January 1, 1970
 * [markedTime] the marked time using the monotonic time source
 * [locationProducerInfo] contains producer's metadata
 */
data class Location(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val speed: Float? = null,
    val altitude: Double? = null,
    val time: Long = 0L,
    val markedTime: TimeSource.Monotonic.ValueTimeMark,
    val locationProducerInfo: LocationProducerInfo
)

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
}

/**
 * A unique and indivisible location producer. For example: The device GPS antenna.
 */
interface LocationProducer {
    val locationFlow: Flow<Location>
}

/**
 * Base class for all concrete types which hold information about location producers.
 */
@Serializable
sealed class LocationProducerInfo

@Serializable
object InternalGps : LocationProducerInfo()

/**
 * Contains all the data which can be used to identify a paired bluetooth device.
 */
@Serializable
data class LocationProducerBtInfo(val name: String, val macAddress: String): LocationProducerInfo()