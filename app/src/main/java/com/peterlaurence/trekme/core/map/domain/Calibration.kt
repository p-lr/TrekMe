package com.peterlaurence.trekme.core.map.domain

import com.peterlaurence.trekme.core.projection.Projection


data class Calibration(
    val projection: Projection?,
    val calibrationMethod: CalibrationMethod,
    val calibrationPoints: List<CalibrationPoint>
)

enum class CalibrationMethod {
    SIMPLE_2_POINTS,
    CALIBRATION_3_POINTS,
    CALIBRATION_4_POINTS
}

/**
 * A calibration point defines a point on the map.
 *
 * Values of [normalizedX] and [normalizedY] are in [0-1] interval. For example, a point which has
 * normalizedX=1 and normalizedY=1 is located at the bottom right corner of the map.
 * A point which has normalizedX=0 and normalizedY=0 is located at the top left corner of the map.
 *
 * Values of [absoluteX] and [absoluteY] can very well be bare latitude and longitude, or projected
 * coordinates.
 */
data class CalibrationPoint(
    var normalizedX: Double,
    var normalizedY: Double,
    var absoluteX: Double,
    var absoluteY: Double
)