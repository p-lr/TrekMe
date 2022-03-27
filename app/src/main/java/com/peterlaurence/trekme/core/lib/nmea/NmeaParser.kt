package com.peterlaurence.trekme.core.lib.nmea

import android.util.Log
import java.util.*

/**
 * Parses NMEA 0183 sentences which are commonly emitted by GPS units.
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
        st.isVTG() -> {
            parseVTG(st)
        }
        st.isGLL() -> {
            parseGLL(st)
        }
        else -> null // Unknown NMEA sentence
    }
}

sealed class NmeaData
data class NmeaGGA(val latitude: Double, val longitude: Double, val elevation: Double? = null, val time: Long) : NmeaData()
data class NmeaRMC(val latitude: Double, val longitude: Double, val speed: Float? = null, val time: Long) : NmeaData()

/**
 * @param speed in meters per seconds
 */
data class NmeaVTG(val speed: Float) : NmeaData()
data class NmeaGLL(val latitude: Double, val longitude: Double) : NmeaData()

private fun String.isGGA(): Boolean = substring(3, 6) == GGA
private fun String.isRMC(): Boolean = substring(3, 6) == RMC
private fun String.isVTG(): Boolean = substring(3, 6) == VTG
private fun String.isGLL(): Boolean = substring(3, 6) == GLL

private fun parseGGA(line: String): NmeaGGA? {
    val fields = line.split(',')

    val epoch = parseHour(fields[1]) ?: run {
        logErr("epoch", line)
        return null
    }
    val lat = parseLatitude(fields.subList(2, 4)) ?: run {
        logErr("lat", line)
        return null
    }
    val lon = parseLongitude(fields.subList(4, 6)) ?: run {
        logErr("lon", line)
        return null
    }

    val eleField = fields[9]
    val ele = if (eleField.isNotBlank()) {
        parseElevation(eleField) ?: run {
            logErr("elevation", line)
            null
        }
    } else null

    return NmeaGGA(lat, lon, elevation = ele, time = epoch)
}

private fun parseRMC(line: String): NmeaRMC? {
    val fields = line.split(',')

    val epoch = parseHour(fields[1]) ?: run {
        logErr("epoch", line)
        return null
    }
    val lat = parseLatitude(fields.subList(3, 5)) ?: run {
        logErr("lat", line)
        return null
    }
    val lon = parseLongitude(fields.subList(5, 7)) ?: run {
        logErr("lon", line)
        return null
    }

    val speedField = fields[7]
    val speed = if (speedField.isNotBlank()) {
        parseGroundSpeed(speedField) ?: run {
            logErr("speed", line)
            return null
        }
    } else null
    return NmeaRMC(lat, lon, speed = speed, time = epoch)
}

private fun parseVTG(line: String): NmeaVTG? {
    val fields = line.split(',')

    val speed = parseGroundSpeed(fields[5]) ?: run {
        logErr("speed", line)
        return null
    }

    return NmeaVTG(speed)
}

private fun parseGLL(line: String): NmeaGLL? {
    val fields = line.split(',')

    val lat = parseLatitude(fields.subList(1, 3)) ?: run {
        logErr("lat", line)
        return null
    }
    val lon = parseLongitude(fields.subList(3, 5)) ?: run {
        logErr("lon", line)
        return null
    }
    return NmeaGLL(lat, lon)
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

private fun parseElevation(field: String): Double? = runCatching {
    field.toDouble()
}.getOrNull()

/**
 * The speed is expected to be expressed in knots.
 * @return The speed in meters per second
 */
private fun parseGroundSpeed(field: String): Float? = runCatching {
    field.toFloat() * KNOTS_TO_METERS_PER_S
}.getOrNull()

private fun getEpoch(hour: Int, minutes: Int, seconds: Int): Long {
    val currentYear = calendar.get(Calendar.YEAR)
    val currentMonth = calendar.get(Calendar.MONTH)
    val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
    calendar.set(currentYear, currentMonth, dayOfMonth, hour, minutes, seconds)
    return calendar.timeInMillis
}

private fun logErr(fieldName: String, line: String) {
    Log.e(TAG, "Failed to parse $fieldName in $line")
}

private val calendar by lazy { GregorianCalendar(TimeZone.getTimeZone("UTC")) }
private const val GGA = "GGA"
private const val RMC = "RMC"
private const val VTG = "VTG"
private const val GLL = "GLL"

private const val KNOTS_TO_METERS_PER_S = 0.514444444f

private const val TAG = "NMEA_parser"