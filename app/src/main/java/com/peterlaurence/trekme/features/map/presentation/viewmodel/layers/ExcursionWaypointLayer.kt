package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import com.peterlaurence.trekme.core.geotools.distanceApprox
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.features.map.domain.interactors.ExcursionInteractor
import com.peterlaurence.trekme.features.map.domain.models.ExcursionWaypointWithNormalizedPos
import com.peterlaurence.trekme.features.map.presentation.ui.components.Marker
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerCallout
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerGrab
import com.peterlaurence.trekme.features.map.presentation.ui.components.makeMarkerSubtitle
import com.peterlaurence.trekme.features.map.presentation.ui.components.markerCalloutHeightDp
import com.peterlaurence.trekme.features.map.presentation.ui.components.markerCalloutWidthDp
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapViewModel
import com.peterlaurence.trekme.features.map.presentation.viewmodel.controllers.positionCallout
import com.peterlaurence.trekme.util.darkenColor
import com.peterlaurence.trekme.util.dpToPx
import com.peterlaurence.trekme.util.map
import com.peterlaurence.trekme.util.parseColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.addCallout
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.centerOnMarker
import ovh.plrapps.mapcompose.api.enableMarkerDrag
import ovh.plrapps.mapcompose.api.getMarkerInfo
import ovh.plrapps.mapcompose.api.moveMarker
import ovh.plrapps.mapcompose.api.removeCallout
import ovh.plrapps.mapcompose.api.removeMarker
import ovh.plrapps.mapcompose.api.updateMarkerClickable
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.UUID

class ExcursionWaypointLayer(
    private val scope: CoroutineScope,
    private val dataStateFlow: Flow<DataState>,
    private val excursionInteractor: ExcursionInteractor,
    private val goToExcursionWaypointFlow: Flow<Pair<ExcursionRef, ExcursionWaypoint>>,
    private val onWaypointEdit: (ExcursionWaypoint, excursionId: String) -> Unit,
    private val onStartItinerary: (ExcursionWaypoint) -> Unit
) : MapViewModel.MarkerTapListener {
    /**
     * Correspondence between excursion ids and their [ExcursionWaypointsState].
     */
    private var excursionWptListState = mutableMapOf<ExcursionRef, ExcursionWaypointsState>()
    private var lastLocation by mutableStateOf<Location?>(null)

    init {
        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                coroutineScope {
                    excursionWptListState.clear()

                    launch {
                        onMapUpdate(map, mapState)
                    }

                    launch {
                        goToExcursionWaypointFlow.collect { (ref, wpt) ->
                            mapState.centerOnMarker(id = makeId(ref.id, wpt.id), destScale = 2f)
                        }
                    }
                }
            }
        }
    }

    fun onLocation(location: Location) {
        lastLocation = location
    }

    private suspend fun onMapUpdate(map: Map, mapState: MapState) {
        map.excursionRefs.collectLatest { refs ->
            coroutineScope {
                for (ref in refs) {
                    launch {
                        ref.visible.collectLatest l@{ visible ->
                            if (ref !in excursionWptListState.keys) {
                                excursionWptListState[ref] = ExcursionWaypointsState(ref)
                            }
                            val state = excursionWptListState[ref] ?: return@l
                            if (visible) {
                                launch {
                                    excursionInteractor.getWaypointsFlow(ref, map).collect { wpts ->
                                        val colorFlow = ref.color.map {
                                            Color(parseColor(it))
                                        }
                                        onExcursionMarkersChange(wpts, mapState, state, colorFlow)
                                    }
                                }
                            } else {
                                state.waypointsState.forEach { (_, u) ->
                                    mapState.removeMarker(u.idOnMap)
                                }
                                excursionWptListState.remove(ref)
                            }
                        }
                    }
                }

                val iter = excursionWptListState.iterator()
                for (entry in iter) {
                    /* The excursion ref has been removed, so remove all corresponding markers */
                    if (entry.key !in refs) {
                        entry.value.waypointsState.forEach { (_, u) ->
                            mapState.removeMarker(u.idOnMap)
                        }
                        iter.remove()
                    }
                }
            }
        }
    }

    private fun onExcursionMarkersChange(
        wpts: List<ExcursionWaypointWithNormalizedPos>,
        mapState: MapState,
        excursionWptState: ExcursionWaypointsState,
        colorFlow: StateFlow<Color>
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
                    colorFlow = colorFlow,
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
            if (entry.key !in ids && entry.value.isStatic) {
                mapState.removeMarker(entry.value.idOnMap)
                iter.remove()
            }
        }
    }

    override fun onMarkerTap(mapState: MapState, mapId: UUID, id: String, x: Double, y: Double): Boolean {
        return when {
            id.startsWith(excursionWptGrabPrefix) -> {
                onExcursionWaypointGrabTap(id, mapState)
                true
            }

            id.startsWith(excursionWaypointPrefix) -> {
                onExcursionWaypointTap(mapState, id, x, y)
            }

            else -> false
        }
    }

    private fun onExcursionWaypointTap(mapState: MapState, id: String, x: Double, y: Double): Boolean {
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
                val distance = lastLocation?.let { distanceApprox(it.latitude, it.longitude, waypoint.latitude, waypoint.longitude) }
                val title = waypoint.name

                MarkerCallout(
                    title = title,
                    subTitle = makeMarkerSubtitle(
                        latitude = waypoint.latitude,
                        longitude = waypoint.longitude,
                        distanceInMeters = distance
                    ),
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
                    },
                    onStartItinerary = {
                        onStartItinerary(waypoint)
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
        colorFlow: StateFlow<Color>,
        mapState: MapState,
        x: Double,
        y: Double
    ): WaypointState {
        val id = makeId(excursionId, waypoint.id)
        val state = WaypointState(id, waypoint)

        mapState.addMarker(
            id,
            x,
            y,
            relativeOffset = Offset(-0.5f, -0.5f),
            zIndex = 1f,
            clickableAreaCenterOffset = Offset(0f, -0.25f),
            clickableAreaScale = Offset(2f, 1f)  // 48dp wide and height
        ) {
            val trackColor by colorFlow.collectAsState()
            val wptColor = state.waypoint.color?.let { Color(parseColor(it)) }
            val color = wptColor ?: trackColor

            Marker(
                isStatic = state.isStatic,
                backgroundColor = color,
                strokeColor = Color(darkenColor(color.toArgb(), 0.15f)),
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

private fun makeId(excursionId: String, waypointId: String) = "$excursionWaypointPrefix-$excursionId|$waypointId"

private const val excursionWaypointPrefix = "excursionWpt"
private const val calloutPrefix = "callout"
private const val excursionWptGrabPrefix = "grabExcursionWpt"

private class ExcursionWaypointsState(val excursionRef: ExcursionRef) {
    val waypointsState = mutableMapOf<String, WaypointState>()
}

private class WaypointState(val idOnMap: String, initWaypoint: ExcursionWaypoint) {
    var waypoint by mutableStateOf<ExcursionWaypoint>(initWaypoint)
    var isStatic by mutableStateOf(true)
}