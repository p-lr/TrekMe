package com.peterlaurence.trekme.lib.nmea

import android.util.Log
import com.peterlaurence.trekme.core.model.Location
import java.util.*

/**
 * Parses the two NMEA 0183 sentences which are commonly emitted by GPS units.
 * @see https://gpsd.gitlab.io/gpsd/NMEA.html#_gtd_geographic_location_in_time_differences
 */
fun parseNmeaLocationSentence(st: String): Location? {
    return when {
        st.isGGA() -> {
            parseGGA(st)
        }
        st.isRMC() -> {
            parseRMC()
        }
        else -> null // Unknown NMEA sentence
    }
}

private fun String.isGGA(): Boolean {
    return substring(3, 6) == GGA
}

private fun String.isRMC(): Boolean {
    return substring(3, 6) == RMC
}

private fun parseGGA(line: String): Location? {
    val fields = line.split(',')

    val epoch = parseHour(fields[1]) ?: run {
        Log.e(TAG, "Failed to parse epoch in $line")
        return null
    }
    val lat = parseLatitude(fields) ?: run {
        Log.e(TAG, "Failed to parse lat in $line")
        return null
    }
    val lon = parseLongitude(fields) ?: run {
        Log.e(TAG, "Failed to parse lon in $line")
        return null
    }
    return Location(lat, lon, time = epoch)
}

private fun parseRMC(): Location? {
    TODO()
}


private fun parseHour(field: String): Long? = runCatching {
    val hour = field.substring(0, 2).toInt()
    val minutes = field.substring(2, 4).toInt()
    val seconds = field.substring(4).substringBefore('.').toInt()
    val milliseconds = field.substringAfter('.', missingDelimiterValue = "0").toInt() * 10
    getEpoch(hour, minutes, seconds) + milliseconds
}.getOrNull()

private fun parseLatitude(fields: List<String>): Double? = runCatching {
    val latField = fields[2]
    val northOrSouth = fields[3]
    val entireDegrees = latField.substring(0, 2).toDouble()
    val minutes = latField.substring(2).toDouble() / 60
    val sign = if (northOrSouth.lowercase() == "n") 1 else -1
    (entireDegrees + minutes) * sign
}.getOrNull()

private fun parseLongitude(fields: List<String>): Double? = runCatching {
    val lonField = fields[4]
    val eastOrWest = fields[5]
    val entireDegrees = lonField.substring(0, 3).toDouble()
    val minutes = lonField.substring(3).toDouble() / 60
    val sign = if (eastOrWest.lowercase() == "e") 1 else -1
    (entireDegrees + minutes) * sign
}.getOrNull()


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

private const val TAG = "NMEA_parser"