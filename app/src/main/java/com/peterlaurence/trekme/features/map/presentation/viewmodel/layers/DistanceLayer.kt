package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.core.geotools.distanceApprox
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.utils.getLonLat
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerGrab
import com.peterlaurence.trekme.util.throttle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.*
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

class DistanceLineState(mapState: MapState, private val map: Map) {
    private val markersSnapshot by mapState.markerDerivedState()
    private val markersSnapshotFlow = mapState.markerSnapshotFlow()

    val marker1Snapshot: MarkerDataSnapshot?
        get() = markersSnapshot.firstOrNull {
            it.id == distMarker1
        }

    val marker2Snapshot: MarkerDataSnapshot?
        get() = markersSnapshot.firstOrNull {
            it.id == distMarker2
        }

    val distanceFlow: Flow<Float> = markersSnapshotFlow.throttle(100).mapNotNull {
        computeDistance(marker1Snapshot, marker2Snapshot)?.toFloat()
    }

    private suspend fun computeDistance(
        marker1: MarkerDataSnapshot?,
        marker2: MarkerDataSnapshot?
    ): Double? = withContext(Dispatchers.Default) {
        if (marker1 == null || marker2 == null) return@withContext null
        val lonLatA = getLonLat(marker1.x, marker1.y, map) ?: return@withContext null
        val lonLatB = getLonLat(marker2.x, marker2.y, map) ?: return@withContext null
        distanceApprox(lonLatA[1], lonLatA[0], lonLatB[1], lonLatB[0])
    }
}

private const val distMarker1 = "distMarker1"
private const val distMarker2 = "distMarker2"