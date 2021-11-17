package com.peterlaurence.trekme.viewmodel.map

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Landmark
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.ui.map.components.LandMark
import com.peterlaurence.trekme.ui.map.components.LandmarkCallout
import com.peterlaurence.trekme.ui.map.components.MarkerGrab
import com.peterlaurence.trekme.ui.map.viewmodel.positionCallout
import com.peterlaurence.trekme.util.dpToPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

class LandmarkLayer(
    private val scope: CoroutineScope,
    private val mapLoader: MapLoader,
    layerData: Flow<LayerData>
) : MapViewModel.MarkerTapListener {
    private var landmarkListState = mapOf<String, LandmarkState>()

    init {
        layerData.map {
            onMapUpdate(it.map, it.mapUiState)
        }.launchIn(scope)
    }

    private suspend fun onMapUpdate(map: Map, mapUiState: MapUiState) {
        val mapBounds = map.mapBounds ?: return

        /* Import landmarks */
        mapLoader.getLandmarksForMap(map)
        val landmarks = map.landmarks ?: return

        landmarkListState = landmarks.map { landmark ->
            val projectedValues = withContext(Dispatchers.Default) {
                map.projection?.doProjection(landmark.lat, landmark.lon)
            } ?: doubleArrayOf(landmark.lon, landmark.lat)

            val id = UUID.randomUUID().toString()
            val x = normalize(projectedValues[0], mapBounds.X0, mapBounds.X1)
            val y = normalize(projectedValues[1], mapBounds.Y0, mapBounds.Y1)

            val state = LandmarkState(id, landmark)
            mapUiState.mapState.addMarker(
                id,
                x,
                y,
                relativeOffset = Offset(-0.5f, -0.5f),
                zIndex = 1f
            ) {
                LandMark(Modifier.padding(5.dp), state.isStatic)
            }
            state
        }.associateBy { it.id }
    }

    override fun onMarkerTap(mapState: MapState, id: String, x: Double, y: Double) {
        if (id.startsWith(markerGrabPrefix)) {
            onMarkerGrabTap(id, mapState)
            return
        }
        val landmarkState = landmarkListState[id] ?: return

        scope.launch {
            var shouldAnimate by mutableStateOf(true)

            val calloutHeight = dpToPx(landmarkCalloutHeightDp).toInt()
            val markerHeight =
                dpToPx(48f).toInt() // The view height is 48dp, but only the top half is used to draw the marker.
            val calloutWidth = dpToPx(landmarkCalloutWidthDp).toInt()
            val markerWidth = dpToPx(24f).toInt()

            val pos = positionCallout(
                mapState,
                calloutWidth,
                calloutHeight,
                x,
                y,
                markerWidth,
                markerHeight
            )

            mapState.addCallout(
                id, x, y,
                relativeOffset = Offset(pos.relativeAnchorLeft, pos.relativeAnchorTop),
                absoluteOffset = Offset(pos.absoluteAnchorLeft, pos.absoluteAnchorTop),
                autoDismiss = true, clickable = false, zIndex = 3f
            ) {
                val subTitle = landmarkListState[id]?.landmark?.let {
                    "${stringResource(id = R.string.latitude_short)} : ${df.format(it.lat)}  " +
                            "${stringResource(id = R.string.longitude_short)} : ${df.format(it.lon)}"
                } ?: ""

                LandmarkCallout(
                    DpSize(landmarkCalloutWidthDp.dp, landmarkCalloutHeightDp.dp),
                    subTitle = subTitle,
                    shouldAnimate,
                    onAnimationDone = { shouldAnimate = false },
                    onDeleteAction = {
                        mapState.removeCallout(id)
                        mapState.removeMarker(id)
                    },
                    onMoveAction = {
                        mapState.removeCallout(id)
                        mapState.updateMarkerClickable(id, false)
                        landmarkState.isStatic = false
                        attachMarkerGrab(id, x, y, mapState, landmarkState)
                    }
                )
            }
        }
    }

    private fun attachMarkerGrab(id: String, xMarker: Double, yMarker: Double, mapState: MapState, landmarkState: LandmarkState) {
        val grabId = "$markerGrabPrefix-$id"
        mapState.addMarker(grabId, xMarker, yMarker, Offset(-0.5f, -0.5f), zIndex = 0f) {
            MarkerGrab(
                morphedIn = !landmarkState.isStatic,
                onMorphOutDone = {
                    mapState.removeMarker(grabId)
                }
            )
        }
        mapState.enableMarkerDrag(grabId) { _, x, y, dx, dy ->
            mapState.moveMarker(grabId, x + dx, y + dy)
            mapState.moveMarker(id, x + dx, y + dy)
        }
    }

    private fun onMarkerGrabTap(markerGrabId: String, mapState: MapState) {
        val markerId = markerGrabId.substringAfter('-')
        val landmarkState = landmarkListState[markerId] ?: return
        landmarkState.isStatic = true
        mapState.updateMarkerClickable(markerId, true)
    }

    private fun normalize(t: Double, min: Double, max: Double): Double {
        return (t - min) / (max - min)
    }
}

private const val markerGrabPrefix = "landmarkGrab"

private data class LandmarkState(val id: String, val landmark: Landmark) {
    var isStatic by mutableStateOf(true)
}

private val df = DecimalFormat("#.####").apply {
    roundingMode = RoundingMode.CEILING
}

private const val landmarkCalloutWidthDp = 140
private const val landmarkCalloutHeightDp = 100
