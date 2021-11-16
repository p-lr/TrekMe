package com.peterlaurence.trekme.viewmodel.map

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Landmark
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.ui.map.components.LandMark
import com.peterlaurence.trekme.ui.map.components.LandmarkCallout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.addCallout
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.removeCallout
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

            mapUiState.mapState.addMarker(id, x, y, relativeOffset = Offset(-0.5f, -0.5f)) {
                LandMark(Modifier.padding(5.dp), isStatic = true)
            }

            LandmarkState(id, landmark)
        }.associateBy { it.id }
    }

    override fun onMarkerTap(mapState: MapState, id: String, x: Double, y: Double) {
        if (id !in landmarkListState.keys) return

        var shouldAnimate by mutableStateOf(true)
        mapState.addCallout(
            id, x, y,
            absoluteOffset = Offset(0f, -80f),
            autoDismiss = true, clickable = false
        ) {
            val subTitle = landmarkListState[id]?.landmark?.let {
                "${stringResource(id = R.string.latitude_short)} : ${df.format(it.lat)}  " +
                        "${stringResource(id = R.string.longitude_short)} : ${df.format(it.lon)}"
            } ?: ""

            LandmarkCallout(
                subTitle = subTitle,
                shouldAnimate,
                onAnimationDone = { shouldAnimate = false },
                onDeleteAction = {
                    // TODO: delete action
                    mapState.removeCallout(id)
                },
                onMoveAction = {
                    // TODO: move action
                    mapState.removeCallout(id)
                }
            )
        }
    }

    private fun normalize(t: Double, min: Double, max: Double): Double {
        return (t - min) / (max - min)
    }
}

private data class LandmarkState(val id: String, val landmark: Landmark)

private val df = DecimalFormat("#.####").apply {
    roundingMode = RoundingMode.CEILING
}