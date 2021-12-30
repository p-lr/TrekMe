package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.MapBounds
import com.peterlaurence.trekme.core.location.Location
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.ui.common.PositionOrientationMarker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState

class LocationLayer(
    private val scope: CoroutineScope,
    private val settings: Settings,
    private val mapFlow: Flow<Map>,
    private val mapStateFlow: Flow<MapState>
) {
    private var hasCenteredOnFirstLocation = false
    private val locationFlow = MutableSharedFlow<Location>(1, 0, BufferOverflow.DROP_OLDEST)

    private val orientationFlow = MutableSharedFlow<Float>(1, 0, BufferOverflow.DROP_OLDEST)
    private val orientationState = mutableStateOf<Float?>(null)
    val isLockedOnPosition = mutableStateOf(false)

    init {
        scope.launch {
            /* At every map or orientation setting change, collect either:
             * - the orientation and location flows, if orientation is enabled,
             * - the location flow only, if orientation is disabled.
             */
            combine(
                mapFlow,
                settings.getOrientationVisibility()
            ) { map, showOrientation ->
                Pair(map, showOrientation)
            }.collectLatest { (map, showOrientation) ->
                val mapState = mapStateFlow.first()
                if (showOrientation) {
                    combine(
                        orientationFlow,
                        locationFlow,
                    ) { orientation, loc ->
                        orientationState.value = orientation
                        onLocation(loc, mapState, map)
                    }.collect()
                } else {
                    locationFlow.collect { loc ->
                        orientationState.value = null
                        onLocation(loc, mapState, map)
                    }
                }
            }
        }

        /* At every map change, set the internal flag */
        mapFlow.map {
            hasCenteredOnFirstLocation = false
        }.launchIn(scope)
    }

    fun onLocation(location: Location) {
        locationFlow.tryEmit(location)
    }

    fun onOrientation(intrinsicAngle: Double, displayRotation: Int) {
        val orientation = (Math.toDegrees(intrinsicAngle) + 360 + displayRotation).toFloat() % 360
        orientationFlow.tryEmit(orientation)
    }

    fun toggleLockedOnPosition() {
        isLockedOnPosition.value = !isLockedOnPosition.value
    }

    fun centerOnPosition() = scope.launch {
        val mapState = mapStateFlow.first()
        centerOnPosMarker(mapState)
    }

    private fun onLocation(location: Location, mapState: MapState, map: Map) {
        scope.launch {
            /* Project lat/lon off UI thread */
            val projectedValues = withContext(Dispatchers.Default) {
                map.projection?.doProjection(location.latitude, location.longitude)
            }

            /* Update the position */
            val mapBounds = map.mapBounds
            if (projectedValues != null) {
                val X = projectedValues[0]
                val Y = projectedValues[1]
                if (mapBounds.contains(X, Y)) {
                    updatePosition(mapState, mapBounds, X, Y)
                }
            }
        }
    }

    private suspend fun updatePosition(
        mapState: MapState,
        mapBounds: MapBounds,
        X: Double,
        Y: Double
    ) {
        updatePositionMarker(mapState, mapBounds, X, Y)

        if (!hasCenteredOnFirstLocation) {
            centerOnPosMarker(mapState)
            hasCenteredOnFirstLocation = true
        }
    }

    private suspend fun centerOnPosMarker(mapState: MapState) {
        val scaleCentered = getScaleCentered().first()
        val defineScaleCentered = settings.getDefineScaleCentered().first()
        if (defineScaleCentered) {
            mapState.centerOnMarker(positionMarkerId, scaleCentered)
        } else {
            mapState.centerOnMarker(positionMarkerId)
        }
    }

    /**
     * Update the position on the map. The first time we update the position, we add the
     * position marker.
     *
     * @param X the projected X coordinate
     * @param Y the projected Y coordinate
     */
    private suspend fun updatePositionMarker(
        mapState: MapState,
        mapBounds: MapBounds,
        X: Double,
        Y: Double
    ) {
        val x = normalize(X, mapBounds.X0, mapBounds.X1)
        val y = normalize(Y, mapBounds.Y0, mapBounds.Y1)

        if (mapState.hasMarker(positionMarkerId)) {
            mapState.moveMarker(positionMarkerId, x, y)
        } else {
            mapState.addMarker(
                positionMarkerId,
                x,
                y,
                relativeOffset = Offset(-0.5f, -0.5f),
                clickable = false
            ) {
                val angle by orientationState
                PositionOrientationMarker(angle = angle)
            }
        }

        if (isLockedOnPosition.value) {
            mapState.scrollTo(x, y)
        }
    }

    private fun normalize(t: Double, min: Double, max: Double): Double {
        return (t - min) / (max - min)
    }

    private fun MapBounds.contains(x: Double, y: Double): Boolean {
        return x in X0..X1 && y in Y1..Y0
    }

    private fun getScaleCentered(): Flow<Float> {
        return settings.getScaleRatioCentered()
            .combine(settings.getMaxScale()) { scaleRatio, maxScale ->
                scaleRatio * maxScale / 100f
            }
    }
}

const val positionMarkerId = "position"