package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.map.domain.models.BoundingBox
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.PositionOrientationMarker
import com.peterlaurence.trekme.features.map.domain.core.getNormalizedCoordinates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

class LocationOrientationLayer(
    private val scope: CoroutineScope,
    private val settings: Settings,
    private val dataStateFlow: Flow<DataState>,
    private val goToBoundingBoxFlow: StateFlow<Pair<UUID, Channel<BoundingBox>>?>,
    private val mapInteractor: MapInteractor,
    private val onOutOfBounds: () -> Unit,
    private val onNoLocation: () -> Unit
) {
    private var hasCenteredOnFirstLocation = false
    val locationFlow = MutableSharedFlow<Location>(1, 0, BufferOverflow.DROP_OLDEST)

    /* Internal angle data flow, which depends the orientation and the display screen angle. */
    private val angleFlow = MutableSharedFlow<Float>(1, 0, BufferOverflow.DROP_OLDEST)

    /* Represents the arrow angle state, which also depends on settings.
     * When the value is null, the orientation arrow isn't displayed. */
    private val arrowAngleState = mutableStateOf<Float?>(null)

    val isLockedOnPosition = mutableStateOf(false)

    init {
        locationFlow.map { loc ->
            val (map, mapState) = dataStateFlow.first()
            onLocation(loc, mapState, map)
        }.launchIn(scope)

        scope.launch {
            /* At every map, orientation setting, and rotation mode change:
             * - collect the angle flow, if orientation is enabled,
             * - hide the orientation arrow and align to the north, if orientation is disabled.
             */
            combine(
                dataStateFlow,
                settings.getOrientationVisibility(),
                settings.getRotationMode()
            ) { dataState, showOrientation, rotationMode ->
                Triple(dataState, showOrientation, rotationMode)
            }.collectLatest { (dataState, showOrientation, rotationMode) ->
                val mapState = dataState.mapState
                applyRotationMode(mapState, rotationMode)

                if (showOrientation) {
                    angleFlow.collect { angle ->
                        if (rotationMode == RotationMode.FOLLOW_ORIENTATION) {
                            dataState.mapState.rotation = -angle
                        }
                        arrowAngleState.value = angle
                    }
                } else {
                    if (rotationMode == RotationMode.FOLLOW_ORIENTATION) {
                        dataState.mapState.rotateTo(0f)
                    }
                    arrowAngleState.value = null
                }
            }
        }

        /* At every map change, set the internal flag */
        dataStateFlow.map {
            hasCenteredOnFirstLocation = false
        }.launchIn(scope)

        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                goToBoundingBoxFlow.collectLatest l@{
                    if (it == null || it.first != map.id) return@l
                    it.second.receiveAsFlow().collectLatest { bb ->
                        // We're about to center on a bounding box. So we don't want to center on
                        // the current position right after.
                        hasCenteredOnFirstLocation = true

                        mapState.scrollToBoundingBox(bb.toMapComposeBoundingBox(map))
                    }
                }
            }
        }
    }

    fun onLocation(location: Location) {
        locationFlow.tryEmit(location)
    }

    fun onOrientation(intrinsicAngle: Double, displayRotation: Int) {
        val orientation = (Math.toDegrees(intrinsicAngle) + 360 + displayRotation).toFloat() % 360
        angleFlow.tryEmit(orientation)
    }

    fun toggleLockedOnPosition() {
        isLockedOnPosition.value = !isLockedOnPosition.value
    }

    fun centerOnPosition() = scope.launch {
        val mapState = dataStateFlow.first().mapState

        val posMarker = mapState.getMarkerInfo(positionMarkerId)
        if (posMarker != null) {
            if (isInMap(posMarker.x, posMarker.y)) {
                centerOnPosMarker(mapState)
            } else {
                onOutOfBounds()
            }
        } else {
            onNoLocation()
        }
    }

    private fun onLocation(location: Location, mapState: MapState, map: Map) {
        scope.launch {
            val normalized =
                mapInteractor.getNormalizedCoordinates(map, location.latitude, location.longitude)

            /* Update the position */
            updatePosition(mapState, normalized.x, normalized.y)
        }
    }

    private suspend fun updatePosition(
        mapState: MapState,
        x: Double,
        y: Double
    ) {
        updatePositionMarker(mapState, x, y)

        if (!hasCenteredOnFirstLocation && isInMap(x, y)) {
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
     * @param x the normalized X coordinate
     * @param y the normalized y coordinate
     */
    private suspend fun updatePositionMarker(
        mapState: MapState,
        x: Double,
        y: Double
    ) {
        if (mapState.hasMarker(positionMarkerId)) {
            mapState.moveMarker(positionMarkerId, x, y)
        } else {
            mapState.addMarker(
                positionMarkerId,
                x,
                y,
                relativeOffset = Offset(-0.5f, -0.5f),
                clickable = false,
                isConstrainedInBounds = false
            ) {
                val angle by arrowAngleState
                PositionOrientationMarker(angle = angle?.let { it + mapState.rotation })
            }
        }

        if (isLockedOnPosition.value) {
            mapState.scrollTo(x, y)
        }
    }

    private fun getScaleCentered(): Flow<Float> {
        return settings.getScaleRatioCentered()
            .combine(settings.getMaxScale()) { scaleRatio, maxScale ->
                scaleRatio * maxScale / 100f
            }
    }

    private fun applyRotationMode(mapState: MapState, rotationMode: RotationMode) {
        when (rotationMode) {
            RotationMode.NONE -> {
                mapState.rotation = 0f
                mapState.disableRotation()
            }
            RotationMode.FOLLOW_ORIENTATION -> {
                mapState.disableRotation()
            }
            RotationMode.FREE -> mapState.enableRotation()
        }
    }

    private fun isInMap(x: Double, y: Double) = x in 0.0..1.0 && y in 0.0..1.0

    private suspend fun MapState.scrollToBoundingBox(boundingBox: ovh.plrapps.mapcompose.api.BoundingBox) {
        scrollTo(boundingBox, padding = Offset(0.2f, 0.2f))
    }

    private suspend fun BoundingBox.toMapComposeBoundingBox(map: Map): ovh.plrapps.mapcompose.api.BoundingBox {
        val (x1, y1) = getNormalizedCoordinates(
            minLat, minLon, map.mapBounds, map.projection,
        )

        val (x2, y2) = getNormalizedCoordinates(
            maxLat, maxLon, map.mapBounds, map.projection,
        )

        return BoundingBox(
            xLeft = min(x1, x2),
            yTop = min(y1, y2),
            xRight = max(x1, x2),
            yBottom = max(y1, y2)
        )
    }
}

const val positionMarkerId = "position"