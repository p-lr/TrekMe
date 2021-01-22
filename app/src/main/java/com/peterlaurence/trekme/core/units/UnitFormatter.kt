package com.peterlaurence.trekme.core.units

import android.text.format.DateUtils

private const val TO_FEET = 1 / 0.3048
private const val TO_YARD = 1 / 0.9144
private const val TO_MILE = 1 / 1609.344

object UnitFormatter {
    var system: MeasurementSystem = MeasurementSystem.METRIC

    /**
     * Given a distance in meters, format this distance to return a value which depends on the
     * measurement system.
     */
    fun formatDistance(dist: Double): String {
        return when (system) {
            MeasurementSystem.METRIC -> {
                if (dist <= 1000) {
                    "%.0f m".format(dist)
                } else {
                    "%.2f km".format(dist / 1000.0)
                }
            }
            MeasurementSystem.IMPERIAL -> {
                val yd = dist * TO_YARD
                if (yd <= 1000) {
                    "%.0f yd".format(yd)
                } else {
                    val mi = dist * TO_MILE
                    "%.2f mi".format(mi)
                }
            }
        }
    }

    /**
     * Format elevation distance. This method only exists because elevation is usually measured in
     * ft in the imperial system, while distance are expresses in miles or yard.
     * In the metric system, all distances are measured in meters.
     */
    fun formatElevation(dist: Double): String {
        return when (system) {
            MeasurementSystem.METRIC -> {
                "%.0f m".format(dist)
            }
            MeasurementSystem.IMPERIAL -> {
                val ft = dist * TO_FEET
                "%.0f ft".format(ft)
            }
        }
    }

    /**
     * Format latitude and longitude in decimal degrees, at 1E-5 precision.
     */
    fun formatLatLon(latOrLon: Double): String {
        return "%.5f°".format(latOrLon)
    }

    fun formatDuration(durationInSec: Long): String {
        return DateUtils.formatElapsedTime(durationInSec)
    }

    /**
     * Given the speed in meters per seconds, format depending on the current measurement system.
     */
    fun formatSpeed(speed: Double): String {
        return when (system) {
            MeasurementSystem.METRIC -> "%.1f km/h".format(speed * 3.6)
            MeasurementSystem.IMPERIAL -> "%.1f mph".format(speed * 2.2369)
        }
    }
}
