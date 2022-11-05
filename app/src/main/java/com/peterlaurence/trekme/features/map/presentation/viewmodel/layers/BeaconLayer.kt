package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.peterlaurence.trekme.core.map.domain.models.Beacon
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.ui.components.Beacon
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerGrab
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.*

class BeaconLayer(
    private val scope: CoroutineScope,
    private val dataStateFlow: Flow<DataState>,
    private val mapInteractor: MapInteractor,
) : MapViewModel.MarkerTapListener {
    private var beaconListState = mutableMapOf<String, BeaconState>()

    init {
        dataStateFlow.map { (map, mapState) ->
            onMapUpdate(map, mapState)
        }.launchIn(scope)
    }

    private suspend fun onMapUpdate(map: Map, mapState: MapState) {
        mapInteractor.getBeaconPositions(map).map { (beacon, x, y) ->
            val state = addBeaconOnMap(beacon, mapState, x, y)
            state
        }.associateBy { it.id }.also {
            beaconListState = it.toMutableMap()
        }
    }

    fun addBeacon() = scope.launch {
        val (map, mapState) = dataStateFlow.first()
        val x = mapState.centroidX
        val y = mapState.centroidY
        val beacon = mapInteractor.addBeacon(map, x, y)
        val beaconState = addBeaconOnMap(beacon, mapState, x, y)
        morphToDynamic(beaconState, x, y, mapState)
        beaconListState[beaconState.id] = beaconState
    }

    override fun onMarkerTap(
        mapState: MapState,
        mapId: UUID,
        id: String,
        x: Double,
        y: Double
    ): Boolean {
        if (id.startsWith(beaconGrabPrefix)) {
            onBeaconGrabTap(id, mapState)
            return true
        }
        return true
    }

    private fun onBeaconGrabTap(beaconGrabId: String, mapState: MapState) {
        val beaconId = beaconGrabId.substringAfter('-')
        val beaconState = beaconListState[beaconId] ?: return
        beaconState.isStatic = true
        mapState.updateMarkerClickable(beaconId, true)

        val markerInfo = mapState.getMarkerInfo(beaconId) ?: return
        val beacon = beaconState.beacon ?: return
        scope.launch {
            dataStateFlow.first().also {
                val updatedBeacon = mapInteractor.updateAndSaveBeacon(
                    beacon,
                    it.map,
                    markerInfo.x,
                    markerInfo.y
                )
                beaconListState[beaconId]?.beacon = updatedBeacon
            }
        }
    }

    private fun addBeaconOnMap(
        beacon: Beacon,
        mapState: MapState,
        x: Double,
        y: Double
    ): BeaconState {
        val id = "$beaconPrefix-${UUID.randomUUID()}"
        val state = BeaconState(id, beacon)
        mapState.addMarker(
            id,
            x,
            y,
            relativeOffset = Offset(-0.5f, -0.5f),
            zIndex = 1f,
            clickableAreaCenterOffset = Offset(0f, -0.22f),
            clickableAreaScale = Offset(0.7f, 0.5f)
        ) {
            Beacon(
                Modifier,
                beaconVicinityRadiusPx = 100f,
                isStatic = state.isStatic,
                scale = mapState.scale
            )
        }
        return state
    }

    private fun morphToDynamic(beaconState: BeaconState, x: Double, y: Double, mapState: MapState) {
        mapState.updateMarkerClickable(beaconState.id, false)
        beaconState.isStatic = false
        attachMarkerGrab(beaconState.id, x, y, mapState, beaconState)
    }

    private fun attachMarkerGrab(
        beaconId: String,
        xMarker: Double,
        yMarker: Double,
        mapState: MapState,
        beaconState: BeaconState
    ) {
        val grabId = "$beaconGrabPrefix-$beaconId"
        mapState.addMarker(grabId, xMarker, yMarker, Offset(-0.5f, -0.5f), zIndex = 0f) {
            MarkerGrab(
                morphedIn = !beaconState.isStatic,
                onMorphOutDone = {
                    mapState.removeMarker(grabId)
                }
            )
        }
        mapState.enableMarkerDrag(grabId) { _, x, y, dx, dy, _, _ ->
            mapState.moveMarker(grabId, x + dx, y + dy)
            mapState.moveMarker(beaconId, x + dx, y + dy)
        }
    }
}

private const val beaconPrefix = "beacon"
private const val beaconGrabPrefix = "grabBeacon"

private data class BeaconState(val id: String, var beacon: Beacon) {
    var isStatic by mutableStateOf(true)
}