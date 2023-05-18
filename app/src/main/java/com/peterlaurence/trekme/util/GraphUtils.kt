package com.peterlaurence.trekme.util

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class NiceScale(private var minPoint: Double, private var maxPoint: Double, private val maxTicks: Int = 10) {
    var tickSpacing = 0.0
    private var range = 0.0
    var niceMin = 0.0
    var niceMax = 0.0

    init {
        calculate()
    }

    /**
     * Calculate and update values for tick spacing and nice
     * minimum and maximum data points on the axis.
     */
    private fun calculate() {
        range = niceNum(maxPoint - minPoint, false)
        tickSpacing = niceNum(range / (maxTicks - 1).coerceAtLeast(1), true)
        niceMin = floor(minPoint / tickSpacing) * tickSpacing
        niceMax = ceil(maxPoint / tickSpacing) * tickSpacing
    }

    /**
     * Returns a "nice" number approximately equal to range Rounds
     * the number if round = true Takes the ceiling if round = false.
     *
     * @param range the data range
     * @param round whether to round the result
     * @return a "nice" number to be used for the data range
     */
    private fun niceNum(range: Double, round: Boolean): Double {
        /** nice, rounded fraction  */
        val exponent: Double = floor(log10(range))
        val fraction = range / 10.0.pow(exponent)
        /** fractional part of range  */
        val niceFraction: Double = if (round) {
            if (fraction < 1.5) 1.0 else if (fraction < 3) 2.0 else if (fraction < 7) 5.0 else 10.0
        } else {
            if (fraction <= 1) 1.0 else if (fraction <= 2) 2.0 else if (fraction <= 5) 5.0 else 10.0
        }
        return niceFraction * 10.0.pow(exponent)
    }
}