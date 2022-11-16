package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.domain.models.Beacon
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.ui.components.Beacon
import com.peterlaurence.trekme.features.map.presentation.ui.components.BeaconClickArea
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerCallout
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerGrab
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapViewModel
import com.peterlaurence.trekme.features.map.presentation.viewmodel.controllers.positionCallout
import com.peterlaurence.trekme.util.dpToPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

class BeaconLayer(
    private val scope: CoroutineScope,
    private val dataStateFlow: Flow<DataState>,
    private val mapInteractor: MapInteractor,
    private val onBeaconEdit: (Beacon, mapId: UUID) -> Unit
) : MapViewModel.MarkerTapListener {
    /**
     * Correspondence between beacon (domain) ids and their associated view state.
     * BeaconState has an "idOnMap" which is correspond to the domain id prefixed with [beaconPrefix].
     * This is useful for click listeners to quickly identify whether the click is done on a beacon
     * or not.
     */
    private var beaconListState = mutableMapOf<String, BeaconState>()

    init {
        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                beaconListState.clear()
                onMapUpdate(map, mapState)
            }
        }
    }

    private suspend fun onMapUpdate(map: Map, mapState: MapState) {
        mapInteractor.getBeaconPositionsFlow(map).collect {
            for (beaconWithNormalizedPos in it) {
                val existing = beaconListState[beaconWithNormalizedPos.beacon.id]
                if (existing != null) {
                    existing.apply {
                        beacon = beaconWithNormalizedPos.beacon
                        mapState.moveMarker(
                            existing.idOnMap,
                            beaconWithNormalizedPos.x,
                            beaconWithNormalizedPos.y
                        )
                    }
                } else {
                    val beaconState = addBeaconOnMap(
                        beaconWithNormalizedPos.beacon,
                        mapState,
                        beaconWithNormalizedPos.x,
                        beaconWithNormalizedPos.y
                    )
                    beaconListState[beaconWithNormalizedPos.beacon.id] = beaconState
                }
            }
            val iter = beaconListState.iterator()
            val ids = it.map { b -> b.beacon.id }
            for (entry in iter) {
                if (entry.key !in ids && entry.value.isStatic) iter.remove()
            }
        }
    }

    fun addBeacon() = scope.launch {
        val (map, mapState) = dataStateFlow.first()
        val x = mapState.centroidX
        val y = mapState.centroidY
        val beacon = mapInteractor.makeBeacon(map, x, y)
        val beaconState = addBeaconOnMap(beacon, mapState, x, y)
        morphToDynamic(beaconState, x, y, mapState)
        beaconListState[beacon.id] = beaconState
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
        val beaconId = id.substringAfter("$beaconPrefix-")
        val beaconState = beaconListState[beaconId] ?: return false

        scope.launch {
            var shouldAnimate by mutableStateOf(true)

            val calloutHeight = dpToPx(beaconCalloutHeightDp).toInt()

            /* Whatever the size of the beacon, the callout will always be positioned at a fixed
             * distance */
            val calloutPadding = dpToPx(48f).toInt()
            val calloutWidth = dpToPx(beaconCalloutWidthDp).toInt()

            val pos = positionCallout(
                mapState,
                calloutWidth,
                calloutHeight,
                x,
                y,
                calloutPadding,
                calloutPadding
            )

            val calloutId = "$calloutPrefix-$beaconId"
            mapState.addCallout(
                calloutId, x, y,
                relativeOffset = Offset(pos.relativeAnchorLeft, pos.relativeAnchorTop),
                absoluteOffset = Offset(pos.absoluteAnchorLeft, pos.absoluteAnchorTop),
                autoDismiss = true, clickable = false, zIndex = 3f
            ) {
                val beacon = beaconListState[beaconId]?.beacon ?: return@addCallout
                val subTitle = beacon.let {
                    "${stringResource(id = R.string.latitude_short)} : ${df.format(it.lat)}  " +
                            "${stringResource(id = R.string.longitude_short)} : ${df.format(it.lon)}"
                }
                val title = beacon.name

                MarkerCallout(
                    DpSize(beaconCalloutWidthDp.dp, beaconCalloutHeightDp.dp),
                    title = title,
                    subTitle = subTitle,
                    shouldAnimate,
                    onAnimationDone = { shouldAnimate = false },
                    onEditAction = {
                        onBeaconEdit(beacon, mapId)
                    },
                    onDeleteAction = {
                        mapState.removeCallout(calloutId)
                        mapState.removeMarker(id)
                        mapState.removeMarker(beaconState.idOnMap)
                        mapInteractor.deleteBeacon(beacon, mapId)
                    },
                    onMoveAction = {
                        mapState.removeCallout(calloutId)
                        morphToDynamic(beaconState, x, y, mapState)
                    }
                )
            }
        }
        return true
    }

    private fun onBeaconGrabTap(beaconGrabId: String, mapState: MapState) {
        val beaconId = beaconGrabId.substringAfter("$beaconPrefix-")
        val beaconState = beaconListState[beaconId] ?: return
        beaconState.isStatic = true

        mapState.updateMarkerClickable(idOfClickArea(beaconState.idOnMap), true)

        val markerInfo = mapState.getMarkerInfo(beaconState.idOnMap) ?: return
        val beacon = beaconState.beacon
        scope.launch {
            dataStateFlow.first().also {
                mapInteractor.updateAndSaveBeacon(
                    beacon,
                    it.map,
                    markerInfo.x,
                    markerInfo.y
                )
            }
        }
    }

    private fun addBeaconOnMap(
        beacon: Beacon,
        mapState: MapState,
        x: Double,
        y: Double
    ): BeaconState {
        val id = "$beaconPrefix-${beacon.id}"
        val state = BeaconState(id, beacon)
        mapState.addMarker(
            id,
            x,
            y,
            relativeOffset = Offset(-0.5f, -0.5f),
            zIndex = 1f,
            clipShape = null,
            clickable = false    // a beacon should not be clickable
        ) {
            Beacon(
                Modifier,
                beaconVicinityRadiusPx = state.beacon.radius,
                isStatic = state.isStatic,
                scale = mapState.scale
            )
        }

        mapState.addMarker(
            idOfClickArea(id),
            x, y,
            relativeOffset = Offset(-0.5f, -0.5f),
            zIndex = 1f,
        ) {
            BeaconClickArea()
        }
        return state
    }

    private fun morphToDynamic(beaconState: BeaconState, x: Double, y: Double, mapState: MapState) {
        mapState.updateMarkerClickable(idOfClickArea(beaconState.idOnMap), false)
        beaconState.isStatic = false
        attachMarkerGrab(beaconState.idOnMap, x, y, mapState, beaconState)
    }

    private fun attachMarkerGrab(
        beaconId: String,
        xMarker: Double,
        yMarker: Double,
        mapState: MapState,
        beaconState: BeaconState
    ) {
        val grabId = idOfGrab(beaconId)
        mapState.addMarker(grabId, xMarker, yMarker, Offset(-0.5f, -0.5f), zIndex = 0f) {
            MarkerGrab(
                morphedIn = !beaconState.isStatic,
                onMorphOutDone = {
                    mapState.removeMarker(grabId)
                }
            )
        }

        val clickableAreaId = idOfClickArea(beaconId)
        mapState.enableMarkerDrag(grabId) { _, x, y, dx, dy, _, _ ->
            mapState.moveMarker(grabId, x + dx, y + dy)
            mapState.moveMarker(beaconId, x + dx, y + dy)
            mapState.moveMarker(clickableAreaId, x + dx, y + dy)
        }
    }

    private fun idOfGrab(beaconId: String) = "$beaconGrabPrefix-$beaconId"
    private fun idOfClickArea(beaconId: String) = "$beaconClickAreaPrefix-$beaconId"
}

private const val beaconPrefix = "beacon"
private const val beaconGrabPrefix = "grabBeacon"
private const val beaconClickAreaPrefix = "beaconClickArea"
private const val calloutPrefix = "callout"

private const val beaconCalloutWidthDp = 200
private const val beaconCalloutHeightDp = 120

private val df = DecimalFormat("#.####").apply {
    roundingMode = RoundingMode.CEILING
}

private class BeaconState(val idOnMap: String, initBeacon: Beacon) {
    var beacon by mutableStateOf<Beacon>(initBeacon)
    var isStatic by mutableStateOf(true)
}