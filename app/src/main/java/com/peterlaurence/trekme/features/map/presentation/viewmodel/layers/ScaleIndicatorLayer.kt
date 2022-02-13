package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.util.dpToPx
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.log10
import kotlin.math.pow

fun makeScaleIndicatorState(
    map: Map,
    mapState: MapState,
    mapInteractor: MapInteractor
): ScaleIndicatorState {
    val width = dpToPx(180).toInt()
    return ScaleIndicatorState(
        mapState,
        width,
        distanceCalculator = object : ScaleIndicatorState.DistanceCalculator {
            /**
             * This distance calculator lazily computes the full width in meters of the map.
             * Since this call is cheap and done once, it's done on the main thread.
             */
            val fullWidthDist by lazy {
                mapInteractor.getMapFullWidthDistance(map) ?: 0.0
            }

            override fun compute(nPx: Int, scale: Float): Double {
                return nPx * fullWidthDist / (scale * map.widthPx)
            }
        }
    )
}

class ScaleIndicatorState(
    val mapState: MapState,
    val widthPx: Int,
    val distanceCalculator: DistanceCalculator
) {
    private var snapScale: Float = mapState.scale
    private var snapWidthRatio = 0f

    val widthRatio: Float
        get() {
            val scale = mapState.scale
            val ratio = scale / snapScale
            return if (snapWidthRatio * ratio in 0.5f..1f) {
                snapWidthRatio * ratio
            } else {
                /* Snap to new value */
                val distance = distanceCalculator.compute(widthPx, scale).toInt()
                val snap = computeSnapValue(distance) ?: 0
                snapScale = scale
                scaleText = UnitFormatter.formatDistance(snap.toDouble())
                (snap.toFloat() / distance).also {
                    snapWidthRatio = it
                }
            }
        }

    var scaleText by mutableStateOf("")

    /**
     * A snap value is an entire multiple of power of 10, which is lower than [input].
     * The first digit of a snap value is either 1, 2, 3, or 5.
     * For example: 835 -> 500, 480 -> 300, 270 -> 200, 114 -> 100
     * The snap value is always greater than half of [input].
     */
    private fun computeSnapValue(input: Int): Int? {
        if (input <= 1) return null

        // Lowest entire power of 10
        val power = (log10(input.toDouble())).toInt()

        val power10 = 10.0.pow(power)
        val mostSignificantDigit = (input / power10).toInt()

        return when {
            mostSignificantDigit >= 5 -> 5 * power10
            mostSignificantDigit >= 3 -> 3 * power10
            mostSignificantDigit >= 2 -> 2 * power10
            else -> power10
        }.toInt()
    }

    fun interface DistanceCalculator {
        fun compute(nPx: Int, scale: Float): Double
    }
}