package com.peterlaurence.trekme.lib.nmea

import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NmeaParserTest {
    private val calendar = GregorianCalendar(TimeZone.getTimeZone("UTC"))

    @Test
    fun should_parse_GGA_sentences() {
        val gga = "\$GPGGA,123519,4807.038,N,01131.324,E,1,08,0.9,545.4,M,46.9,M, , *42"
        val loc = parseNmeaLocationSentence(gga)

        assertNotNull(loc)
        calendar.timeInMillis = loc.time
        assertEquals(12, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(35, calendar.get(Calendar.MINUTE))
        assertEquals(19, calendar.get(Calendar.SECOND))
        assertEquals(48.1173, loc.latitude)
        assertEquals(11.522066667, loc.longitude, 1E-8)
        assertNull(loc.speed)
    }
}