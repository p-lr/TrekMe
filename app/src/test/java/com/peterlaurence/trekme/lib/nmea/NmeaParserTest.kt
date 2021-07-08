package com.peterlaurence.trekme.lib.nmea

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class NmeaParserTest {
    private val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"))

    @Test
    fun should_parse_GGA_sentences() {
        val gga = "\$GPGGA,123519,4807.038,N,01131.324,E,1,08,0.9,545.4,M,46.9,M, , *42"
        val loc = parseNmeaLocationSentence(gga)

        assertNotNull(loc)
        assertIs<NmeaGGA>(loc)
        calendar.timeInMillis = loc.time
        assertEquals(12, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(35, calendar.get(Calendar.MINUTE))
        assertEquals(19, calendar.get(Calendar.SECOND))
        assertEquals(48.1173, loc.latitude)
        assertEquals(11.522066667, loc.longitude, 1E-8)
        assertEquals(545.4, loc.elevation)
    }

    @Test
    fun should_parse_RMC_sentences() {
        val rmc = "\$GPRMC,225446,A,4916.45,N,12311.12,W,12.4,054.7,191194,020.3,E*68"
        val loc = parseNmeaLocationSentence(rmc)

        assertNotNull(loc)
        assertIs<NmeaRMC>(loc)
        calendar.timeInMillis = loc.time
        assertEquals(22, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(54, calendar.get(Calendar.MINUTE))
        assertEquals(46, calendar.get(Calendar.SECOND))
        assertEquals(49.274166667, loc.latitude, 1E-8)
        assertEquals(-123.185333333, loc.longitude, 1E-8)
        assertNotNull(loc.speed)
        assertEquals(6.37911056f, loc.speed, 1E-6f)
    }

    @Test
    fun should_parse_VTG_sentences() {
        val vtg = "\$GPVTG,054.7,T,034.4,M,005.5,N,010.2,K"
        val data = parseNmeaLocationSentence(vtg)

        assertNotNull(data)
        assertIs<NmeaVTG>(data)
        assertEquals(54.7f, data.bearing)
        assertEquals(2.82944442f, data.speed, 1E-6f)
    }

    @Test
    fun should_parse_GLL_sentences() {
        val gll = "\$GPGLL,4916.45,N,12311.12,W,225444,A"
        val data = parseNmeaLocationSentence(gll)

        assertNotNull(data)
        assertIs<NmeaGLL>(data)
        assertEquals(49.274166667, data.latitude, 1E-9)
        assertEquals(-123.185333333, data.longitude, 1E-9)
    }
}