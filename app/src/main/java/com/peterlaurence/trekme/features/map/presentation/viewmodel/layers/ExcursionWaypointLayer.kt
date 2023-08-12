package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.features.map.domain.interactors.ExcursionInteractor
import com.peterlaurence.trekme.features.map.domain.models.ExcursionWaypointWithNormalizedPos
import com.peterlaurence.trekme.features.map.presentation.ui.components.Marker
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerCallout
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerGrab
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapViewModel
import com.peterlaurence.trekme.features.map.presentation.viewmodel.controllers.positionCallout
import com.peterlaurence.trekme.util.dpToPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

class ExcursionWaypointLayer(
    private val scope: CoroutineScope,
    private val dataStateFlow: Flow<DataState>,
    private val excursionInteractor: ExcursionInteractor,
    private val onWaypointEdit: (ExcursionWaypoint, excursionId: String) -> Unit
) : MapViewModel.MarkerTapListener {
    /**
     * Correspondence between excursion ids and their [ExcursionWaypointsState].
     */
    private var excursionWptListState = mutableMapOf<ExcursionRef, ExcursionWaypointsState>()

    init {
        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                excursionWptListState.clear()
                onMapUpdate(map, mapState)
            }
        }
    }

    private suspend fun onMapUpdate(map: Map, mapState: MapState) {
        map.excursionRefs.collectLatest { refs ->
            coroutineScope {
                for (ref in refs) {
                    if (ref !in excursionWptListState.keys) {
                        excursionWptListState[ref] = ExcursionWaypointsState(ref)
                    }
                    val state = excursionWptListState[ref] ?: continue
                    launch {
                        excursionInteractor.getWaypointsFlow(ref, map).collect { wpts ->
                            onExcursionMarkersChange(wpts, mapState, state)
                        }
                    }
                }

                val iter = excursionWptListState.iterator()
                for (entry in iter) {
                    if (entry.key !in refs) iter.remove()
                }
            }
        }
    }

    private fun onExcursionMarkersChange(
        wpts: List<ExcursionWaypointWithNormalizedPos>,
        mapState: MapState,
        excursionWptState: ExcursionWaypointsState
    ) {
        for (wptWithNormalizedPos in wpts) {
            val existing = excursionWptState.waypointsState[wptWithNormalizedPos.waypoint.id]
            if (existing != null) {
                existing.apply {
                    waypoint = wptWithNormalizedPos.waypoint
                    mapState.moveMarker(
                        existing.idOnMap,
                        wptWithNormalizedPos.x,
                        wptWithNormalizedPos.y
                    )
                }
            } else {
                val state = addExcursionWaypointOnMap(
                    waypoint = wptWithNormalizedPos.waypoint,
                    excursionId = excursionWptState.excursionRef.id,
                    mapState = mapState,
                    x = wptWithNormalizedPos.x,
                    y = wptWithNormalizedPos.y
                )
                excursionWptState.waypointsState[wptWithNormalizedPos.waypoint.id] = state
            }
        }
        val iter = excursionWptState.waypointsState.iterator()
        val ids = wpts.map { b -> b.waypoint.id }
        for (entry in iter) {
            if (entry.key !in ids && entry.value.isStatic) iter.remove()
        }
    }

    override fun onMarkerTap(mapState: MapState, mapId: UUID, id: String, x: Double, y: Double): Boolean {
        return when {
            id.startsWith(excursionWptGrabPrefix) -> {
                onExcursionWaypointGrabTap(id, mapState)
                true
            }
            id.startsWith(excursionWaypointPrefix) -> {
                onExcursionWaypointTap(mapState, mapId, id, x, y)
            }
            else -> false
        }
    }

    private fun onExcursionWaypointTap(mapState: MapState, mapId: UUID, id: String, x: Double, y: Double): Boolean {
        val payload = id.substringAfter("$excursionWaypointPrefix-")
        val ids = payload.split("|")
        if (ids.size != 2) return false
        val (excursionId, waypointId) = ids

        val wptState = excursionWptListState.firstNotNullOfOrNull {
            if (it.key.id == excursionId) {
                it.value.waypointsState[waypointId]
            } else null
        } ?: return false

        scope.launch {
            var shouldAnimate by mutableStateOf(true)

            val calloutHeight = dpToPx(markerCalloutHeightDp).toInt()
            val markerHeight =
                dpToPx(48f).toInt() // The view height is 48dp, but only the top half is used to draw the marker.
            val calloutWidth = dpToPx(markerCalloutWidthDp).toInt()
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

            val calloutId = "$calloutPrefix-$waypointId"
            mapState.addCallout(
                calloutId, x, y,
                relativeOffset = Offset(pos.relativeAnchorLeft, pos.relativeAnchorTop),
                absoluteOffset = Offset(pos.absoluteAnchorLeft, pos.absoluteAnchorTop),
                autoDismiss = true, clickable = false, zIndex = 3f
            ) {
                val waypoint = wptState.waypoint
                val subTitle = waypoint.let {
                    "${stringResource(id = R.string.latitude_short)} : ${df.format(it.latitude)}  " +
                            "${stringResource(id = R.string.longitude_short)} : ${df.format(it.longitude)}"
                }
                val title = waypoint.name

                MarkerCallout(
                    DpSize(markerCalloutWidthDp.dp, markerCalloutHeightDp.dp),
                    title = title,
                    subTitle = subTitle,
                    shouldAnimate,
                    onAnimationDone = { shouldAnimate = false },
                    onEditAction = {
                        onWaypointEdit(waypoint, excursionId)
                    },
                    onDeleteAction = {
                        mapState.removeCallout(calloutId)
                        mapState.removeMarker(wptState.idOnMap)
                        scope.launch {
                            excursionInteractor.deleteWaypoint(excursionId, waypoint)
                        }
                    },
                    onMoveAction = {
                        mapState.removeCallout(calloutId)
                        morphToDynamic(wptState, x, y, mapState)
                    }
                )
            }
        }
        return true
    }

    private fun attachMarkerGrab(
        waypointId: String,
        xMarker: Double,
        yMarker: Double,
        mapState: MapState,
        waypointState: WaypointState
    ) {
        val grabId = "$excursionWptGrabPrefix-$waypointId"
        mapState.addMarker(grabId, xMarker, yMarker, Offset(-0.5f, -0.5f), zIndex = 0f) {
            MarkerGrab(
                morphedIn = !waypointState.isStatic,
                onMorphOutDone = {
                    mapState.removeMarker(grabId)
                }
            )
        }
        mapState.enableMarkerDrag(grabId) { _, x, y, dx, dy, _, _ ->
            mapState.moveMarker(grabId, x + dx, y + dy)
            mapState.moveMarker(waypointId, x + dx, y + dy)
        }
    }

    private fun onExcursionWaypointGrabTap(markerGrabId: String, mapState: MapState) {
        val payload = markerGrabId.substringAfter("$excursionWptGrabPrefix-$excursionWaypointPrefix-")
        val ids = payload.split("|")
        if (ids.size != 2) return
        val (excursionId, waypointId) = ids

        val wptState = excursionWptListState.firstNotNullOfOrNull {
            if (it.key.id == excursionId) {
                it.value.waypointsState[waypointId]
            } else null
        } ?: return

        wptState.isStatic = true

        mapState.updateMarkerClickable(wptState.idOnMap, true)

        val markerInfo = mapState.getMarkerInfo(wptState.idOnMap) ?: return
        val waypoint = wptState.waypoint
        scope.launch {
            dataStateFlow.first().also {
                excursionInteractor.updateAndSaveWaypoint(
                    excursionId = excursionId,
                    map = it.map,
                    waypoint = waypoint,
                    x = markerInfo.x,
                    y = markerInfo.y
                )
            }
        }
    }

    private fun addExcursionWaypointOnMap(
        waypoint: ExcursionWaypoint,
        excursionId: String,
        mapState: MapState,
        x: Double,
        y: Double
    ): WaypointState {
        val id = "$excursionWaypointPrefix-$excursionId|${waypoint.id}"
        val state = WaypointState(id, waypoint)

        mapState.addMarker(
            id,
            x,
            y,
            relativeOffset = Offset(-0.5f, -0.5f),
            zIndex = 1f,
            clickableAreaCenterOffset = Offset(0f, -0.22f),
            clickableAreaScale = Offset(0.7f, 0.5f)
        ) {
            Marker(
                modifier = Modifier.padding(5.dp),
                isStatic = state.isStatic,
                backgroundColor =  Color(0xFF78909C),
                strokeColor = Color(0xFF607C8C),
            )
        }
        return state
    }

    private fun morphToDynamic(waypointState: WaypointState, x: Double, y: Double, mapState: MapState) {
        mapState.updateMarkerClickable(waypointState.idOnMap, false)
        waypointState.isStatic = false
        attachMarkerGrab(waypointState.idOnMap, x, y, mapState, waypointState)
    }
}

private const val excursionWaypointPrefix = "excursionWpt"
private const val calloutPrefix = "callout"
private const val excursionWptGrabPrefix = "grabExcursionWpt"

private const val markerCalloutWidthDp = 200
private const val markerCalloutHeightDp = 120

private val df = DecimalFormat("#.####").apply {
    roundingMode = RoundingMode.CEILING
}

private class ExcursionWaypointsState(val excursionRef: ExcursionRef) {
    val waypointsState = mutableMapOf<String, WaypointState>()
}

private class WaypointState(val idOnMap: String, initWaypoint: ExcursionWaypoint) {
    var waypoint by mutableStateOf<ExcursionWaypoint>(initWaypoint)
    var isStatic by mutableStateOf(true)
}