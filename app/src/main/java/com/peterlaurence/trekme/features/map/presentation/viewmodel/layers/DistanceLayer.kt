package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerGrab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.enableMarkerDrag
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.visibleArea
import ovh.plrapps.mapcompose.ui.state.MapState

class DistanceLayer(
    private val scope: CoroutineScope,
    private val mapStateFlow: Flow<MapState>,
) {
    var isVisible = MutableStateFlow(false)
        private set

    init {
        mapStateFlow.map {
            isVisible.value = false
            hide(it)
        }.launchIn(scope)
    }

    fun toggleDistance() = scope.launch {
        mapStateFlow.first().also { mapState ->
            if (isVisible.value) {
                hide(mapState)
            } else {
                show(mapState)
            }
        }
    }

    private suspend fun show(mapState: MapState) {
        val area = mapState.visibleArea()
        val p1x = (0.75 * area.p1x + 0.25 * area.p3x).coerceIn(0.0..1.0)
        val p1y = (0.75 * area.p1y + 0.25 * area.p3y).coerceIn(0.0..1.0)
        val p2x = (0.25 * area.p1x + 0.75 * area.p3x).coerceIn(0.0..1.0)
        val p2y = (0.25 * area.p1y + 0.75 * area.p3y).coerceIn(0.0..1.0)

        mapState.addMarker(distMarker1, p1x, p1y, relativeOffset = Offset(-0.5f, -0.5f)) {
            MarkerGrab(morphedIn = true, size = 50.dp)
        }

        mapState.addMarker(distMarker2, p2x, p2y, relativeOffset = Offset(-0.5f, -0.5f)) {
            MarkerGrab(morphedIn = true, size = 50.dp)
        }

        mapState.enableMarkerDrag(distMarker1)
        mapState.enableMarkerDrag(distMarker2)

        isVisible.value = true
    }

    private fun hide(mapState: MapState) {
        mapState.removeMarker(distMarker1)
        mapState.removeMarker(distMarker2)

        isVisible.value = false
    }
}

private const val distMarker1 = "distMarker1"
private const val distMarker2 = "distMarker2"