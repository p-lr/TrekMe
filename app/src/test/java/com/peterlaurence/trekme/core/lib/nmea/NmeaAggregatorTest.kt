package com.peterlaurence.trekme.core.lib.nmea

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Simulate running the [NmeaAggregator] with an incoming flow of [NmeaData].
 * We check the emitted [Location]s. A new [Location] should be emitted only when we have at least
 * latitude and longitude data (less than a predefined timeout old). Other data such as speed and
 * elevation are optional, and should also be less than "timeout" old.
 */
class NmeaAggregatorTest {

    @Test
    fun should_honor_timeout() = runBlocking {
        val dataFlow: Flow<NmeaData> = flow {
            emit(NmeaGLL(12.2, 5.5))
            emit(NmeaRMC(12.3, 5.6, 2.7f, 458764))
            delay(2500) // simulate a long pause
            emit(NmeaVTG(2.8f))  // that shouldn't result in emitting a new Location
            emit(NmeaGLL(11.2, 5.4))
            delay(1500)
            emit(NmeaGGA(14.7, 5.1, 487.8, 98463))
            delay(2200) // simulate a long pause
            emit(NmeaGLL(14.8, 5.2))
        }

        val locations = mutableListOf<LocationData>()
        NmeaAggregator(dataFlow, timeoutMillis = 2000) { lat, lon, speed, altitude, time ->
            locations.add(LocationData(lat, lon, speed, altitude, time))
        }.run()

        assertEquals(5, locations.size)
        assertNull(locations[0].speed)
        assertEquals(2.7f, locations[1].speed)
        assertEquals(11.2, locations[2].latitude)
        assertEquals(2.8f, locations[2].speed)
        assertEquals(2.8f, locations[3].speed) // the last emitted speed was 1.5s ago, still valid
        assertEquals(98463, locations[3].time)
        assertEquals(14.8, locations[4].latitude)
        assertNull(locations[4].speed)  // the last emitted speed was discarded (timeout)
    }
}

private class LocationData(val latitude: Double = 0.0, val longitude: Double = 0.0, val speed: Float? = null,
                           val altitude: Double? = null, val time: Long = 0L)