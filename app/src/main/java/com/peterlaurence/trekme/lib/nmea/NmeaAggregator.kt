package com.peterlaurence.trekme.lib.nmea

import com.peterlaurence.trekme.core.model.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect

/**
 * Aggregates data coming from NMEA sentences and emits [Location]s when it has enough information.
 * [timeoutMillis] defines how long a received data is considered valid.
 */
class NmeaAggregator(private val input: Flow<NmeaData>, timeoutMillis: Int = 2000,
                     val onLocation: (Location) -> Unit) {
    private val timeoutNanos = timeoutMillis * 1_000_000
    private var latLonTimed = LatLonTimedData()
    private var altitude = SingleTimedData<Double>()
    private var speedTimed = SingleTimedData<Float>()
    private var time: Long = System.currentTimeMillis()

    suspend fun run() {
        input.collect { data ->
            val now = System.nanoTime()
            when (data) {
                is NmeaGGA -> {
                    latLonTimed.apply {
                        lat = data.latitude
                        lon = data.longitude
                        timeStamp = now
                    }
                    altitude.apply {
                        this.data = data.elevation
                        timeStamp = now
                    }
                    time = data.time
                }
                is NmeaGLL -> {
                    latLonTimed.apply {
                        lat = data.latitude
                        lon = data.longitude
                        timeStamp = now
                    }
                    time = System.currentTimeMillis()
                }
                is NmeaRMC -> {
                    latLonTimed.apply {
                        lat = data.latitude
                        lon = data.longitude
                        timeStamp = now
                    }
                    speedTimed.apply {
                        this.data = data.speed
                        timeStamp = now
                    }
                    time = data.time
                }
                is NmeaVTG -> {
                    speedTimed.apply {
                        this.data = data.speed
                        timeStamp = now
                    }
                    time = System.currentTimeMillis()
                }
            }
            tryEmit(now)
        }
    }

    private fun tryEmit(now: Long) {
        val lat = latLonTimed.lat ?: return
        val lon = latLonTimed.lon ?: return
        latLonTimed.timeStamp?.also {
            if ((now - it) > timeoutNanos) return
        }
        val speed = speedTimed.data?.takeIf {
            speedTimed.timeStamp?.let {
                now - it < timeoutNanos
            } ?: false
        }
        val altitude = altitude.data?.takeIf {
            altitude.timeStamp?.let {
                now - it < timeoutNanos
            } ?: false
        }
        onLocation(Location(lat, lon, speed, altitude, time))
    }
}

private class SingleTimedData<T>(var data: T? = null, var timeStamp: Long? = null)
private class LatLonTimedData(var lat: Double? = null, var lon: Double? = null, var timeStamp: Long? = null)
