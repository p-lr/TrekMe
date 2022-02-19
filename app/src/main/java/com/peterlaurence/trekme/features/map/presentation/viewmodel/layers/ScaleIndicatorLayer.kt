package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.units.MeasurementSystem
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.util.dpToPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.log10
import kotlin.math.pow

class ScaleIndicatorLayer(
    scope: CoroutineScope,
    private val isShowingScaleIndicator: Flow<Boolean>,
    private val measurementSystemFlow: Flow<MeasurementSystem>,
    private val dataStateFlow: Flow<DataState>,
    private val mapInteractor: MapInteractor,
) {
    val state = ScaleIndicatorState(dpToPx(180).toInt())

    init {
        scope.launch {
            isShowingScaleIndicator.collectLatest { isShowing ->
                if (!isShowing) return@collectLatest
                dataStateFlow.collectLatest { (map, mapState) ->
                    measurementSystemFlow.collectLatest { measurementSystem ->
                        updateScaleIndicator(map, mapState, measurementSystem)
                    }
                }
            }
        }
    }

    private suspend fun updateScaleIndicator(
        map: Map,
        mapState: MapState,
        measurementSystem: MeasurementSystem
    ) {
        val distanceCalculator = object : DistanceCalculator {
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

        val scaleFlow = snapshotFlow {
            mapState.scale
        }

        var snapScale: Float = mapState.scale
        var snapWidthRatio = 0f

        scaleFlow.collect { scale ->
            val ratio = scale / snapScale
            state.widthRatio = if (snapWidthRatio * ratio in 0.5f..1f) {
                snapWidthRatio * ratio
            } else {
                /* Snap to new value */
                when (measurementSystem) {
                    MeasurementSystem.METRIC -> {
                        val distance = distanceCalculator.compute(state.widthPx, scale).toInt()
                        val snap = computeSnapValue(distance)
                        snapScale = scale
                        state.scaleText = UnitFormatter.formatDistance(snap.toDouble(), precision = 0u)
                        (snap.toFloat() / distance).also {
                            snapWidthRatio = it
                        }
                    }
                    MeasurementSystem.IMPERIAL -> {
                        val distance = distanceCalculator.compute(state.widthPx, scale)
                        val yards = distance / 0.9144
                        val (imperial, unit) = if (yards < 2000) {
                            Pair(yards, "yd")
                        } else {
                            Pair(yards / 1760, "mi")
                        }
                        val snap = computeSnapValue(imperial.toInt())
                        snapScale = scale
                        state.scaleText = "$snap $unit"
                        (snap / imperial).toFloat().also {
                            snapWidthRatio = it
                        }
                    }
                }
            }
        }
    }

    /**
     * A snap value is an entire multiple of power of 10, which is lower than [input].
     * The first digit of a snap value is either 1, 2, 3, or 5.
     * For example: 835 -> 500, 480 -> 300, 270 -> 200, 114 -> 100
     * The snap value is always greater than half of [input].
     */
    private fun computeSnapValue(input: Int): Int {
        if (input <= 1) return 1

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

    private fun interface DistanceCalculator {
        fun compute(nPx: Int, scale: Float): Double
    }
}

class ScaleIndicatorState(
    val widthPx: Int
) {
    var widthRatio by mutableStateOf(1f)
    var scaleText by mutableStateOf("")
}