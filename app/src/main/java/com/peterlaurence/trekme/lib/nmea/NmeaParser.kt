package com.peterlaurence.trekme.lib.nmea

import android.util.Log
import java.util.*

/**
 * Parses the two NMEA 0183 sentences which are commonly emitted by GPS units.
 * @see https://gpsd.gitlab.io/gpsd/NMEA.html#_gtd_geographic_location_in_time_differences
 */
fun parseNmeaLocationSentence(st: String): NmeaData? {
    return when {
        st.isGGA() -> {
            parseGGA(st)
        }
        st.isRMC() -> {
            parseRMC(st)
        }
        else -> null // Unknown NMEA sentence
    }
}

sealed class NmeaData
data class NmeaGGA(val latitude: Double, val longitude: Double, val elevation: Double, val time: Long): NmeaData()
data class NmeaRMC(val latitude: Double, val longitude: Double, val speed: Float, val time: Long): NmeaData()

private fun String.isGGA(): Boolean {
    return substring(3, 6) == GGA
}

private fun String.isRMC(): Boolean {
    return substring(3, 6) == RMC
}

private fun parseGGA(line: String): NmeaData? {
    val fields = line.split(',')

    val epoch = parseHour(fields[1]) ?: run {
        Log.e(TAG, "Failed to parse epoch in $line")
        return null
    }
    val lat = parseLatitude(fields.subList(2, 4)) ?: run {
        Log.e(TAG, "Failed to parse lat in $line")
        return null
    }
    val lon = parseLongitude(fields.subList(4, 6)) ?: run {
        Log.e(TAG, "Failed to parse lon in $line")
        return null
    }
    val ele = parseElevation(fields[9])

    return NmeaGGA(lat, lon, elevation = ele, time = epoch)
}

private fun parseRMC(line: String): NmeaData? {
    val fields = line.split(',')

    val epoch = parseHour(fields[1]) ?: run {
        Log.e(TAG, "Failed to parse epoch in $line")
        return null
    }
    val lat = parseLatitude(fields.subList(3, 5)) ?: run {
        Log.e(TAG, "Failed to parse lat in $line")
        return null
    }
    val lon = parseLongitude(fields.subList(5, 7)) ?: run {
        Log.e(TAG, "Failed to parse lon in $line")
        return null
    }
    val speed = parseGroundSpeed(fields[7])
    return NmeaRMC(lat, lon, speed = speed, time = epoch)
}


private fun parseHour(field: String): Long? = runCatching {
    val hour = field.substring(0, 2).toInt()
    val minutes = field.substring(2, 4).toInt()
    val seconds = field.substring(4).substringBefore('.').toInt()
    val milliseconds = field.substringAfter('.', missingDelimiterValue = "0").toInt() * 10
    getEpoch(hour, minutes, seconds) + milliseconds
}.getOrNull()

private fun parseLatitude(fields: List<String>): Double? = runCatching {
    val latField = fields[0]
    val northOrSouth = fields[1]
    val entireDegrees = latField.substring(0, 2).toDouble()
    val minutes = latField.substring(2).toDouble() / 60
    val sign = if (northOrSouth.lowercase() == "n") 1 else -1
    (entireDegrees + minutes) * sign
}.getOrNull()

private fun parseLongitude(fields: List<String>): Double? = runCatching {
    val lonField = fields[0]
    val eastOrWest = fields[1]
    val entireDegrees = lonField.substring(0, 3).toDouble()
    val minutes = lonField.substring(3).toDouble() / 60
    val sign = if (eastOrWest.lowercase() == "e") 1 else -1
    (entireDegrees + minutes) * sign
}.getOrNull()

private fun parseElevation(field: String): Double {
    return field.toDouble()
}

/**
 * The speed is expected to be expressed in knots.
 * @return The speed in meters per second
 */
private fun parseGroundSpeed(field: String): Float {
    return field.toFloat() * KNOTS_TO_METERS_PER_S
}

private fun getEpoch(hour: Int, minutes: Int, seconds: Int): Long {
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    calendar.set(currentYear, currentMonth, dayOfMonth, hour, minutes, seconds)
    return calendar.timeInMillis
}

private val calendar by lazy { GregorianCalendar(TimeZone.getTimeZone("UTC")) }
private const val GGA = "GGA"
private const val RMC = "RMC"
private const val KNOTS_TO_METERS_PER_S = 0.514444444f

private const val TAG = "NMEA_parser"