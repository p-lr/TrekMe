package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.peterlaurence.trekme.core.geotools.distanceApprox
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.features.map.domain.interactors.MarkerInteractor
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
import com.peterlaurence.trekme.util.parseColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.*

class MarkerLayer(
    private val scope: CoroutineScope,
    private val dataStateFlow: Flow<DataState>,
    private val markerInteractor: MarkerInteractor,
    private val goToMarkerFlow: Flow<Marker>,
    private val onMarkerEdit: (Marker, UUID) -> Unit,
    private val onStartItinerary: (Marker) -> Unit
) : MapViewModel.MarkerTapListener {
    /**
     * Correspondence between marker (domain) ids and their associated view state.
     * MarkerState has an "idOnMap" which is correspond to the domain id prefixed with [markerPrefix].
     * This is useful for click listeners to quickly identify whether the click is done on a marker
     * or not.
     */
    private var markerListState = mutableMapOf<String, MarkerState>()
    private var lastLocation by mutableStateOf<Location?>(null)

    init {
        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                coroutineScope {
                    markerListState.clear()
                    launch {
                        onMapUpdate(map, mapState)
                    }
                    launch {
                        goToMarkerFlow.collect { marker ->
                            mapState.setVisibleAreaPadding(bottomRatio = 0f)
                            mapState.centerOnMarker(id = makeId(marker), destScale = 2f)
                        }
                    }
                }
            }
        }
    }

    fun addMarker() = scope.launch {
        val (map, mapState) = dataStateFlow.firstOrNull() ?: return@launch
        val x = mapState.centroidX
        val y = mapState.centroidY
        val marker = markerInteractor.makeMarker(map, x, y)
        val markerState = addMarkerOnMap(marker, mapState, x, y)
        morphToDynamic(markerState, x, y, mapState)
        markerListState[marker.id] = markerState
    }

    fun onLocation(location: Location) {
        lastLocation = location
    }

    private suspend fun onMapUpdate(map: Map, mapState: MapState) {
        markerInteractor.getMarkersFlow(map).collect {
            for (markerWithNormalizedPos in it) {
                val existing = markerListState[markerWithNormalizedPos.marker.id]
                if (existing != null) {
                    // TODO: this is called even when the position hasn't changed. Instead, a Marker
                    // could have observable properties.
                    existing.apply {
                        marker = markerWithNormalizedPos.marker
                        mapState.moveMarker(
                            existing.idOnMap,
                            markerWithNormalizedPos.x,
                            markerWithNormalizedPos.y
                        )
                    }
                } else {
                    val markerState = addMarkerOnMap(
                        markerWithNormalizedPos.marker,
                        mapState,
                        markerWithNormalizedPos.x,
                        markerWithNormalizedPos.y
                    )
                    markerListState[markerWithNormalizedPos.marker.id] = markerState
                }
            }
            val iter = markerListState.iterator()
            val ids = it.map { b -> b.marker.id }
            for (entry in iter) {
                if (entry.key !in ids && entry.value.isStatic) {
                    mapState.removeMarker(entry.value.idOnMap)
                    iter.remove()
                }
            }
        }
    }

    override fun onMarkerTap(mapState: MapState, mapId: UUID, id: String, x: Double, y: Double): Boolean {
        if (id.startsWith(markerGrabPrefix)) {
            onMarkerGrabTap(id, mapState)
            return true
        }
        val markerId = id.substringAfter("$markerPrefix-")
        val markerState = markerListState[markerId] ?: return false

        scope.launch {
            var shouldAnimate by mutableStateOf(true)

            val calloutHeight = dpToPx(markerCalloutHeightDp).toInt()  // the actual height might be greater than this fixed height, but the resulting positioning should be good enough
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

            val calloutId = "$calloutPrefix-$markerId"
            mapState.addCallout(
                calloutId, x, y,
                relativeOffset = Offset(pos.relativeAnchorLeft, pos.relativeAnchorTop),
                absoluteOffset = Offset(pos.absoluteAnchorLeft, pos.absoluteAnchorTop),
                autoDismiss = true, clickable = false, zIndex = 3f
            ) {
                val marker = markerListState[markerId]?.marker ?: return@addCallout
                val distance = lastLocation?.let { distanceApprox(it.latitude, it.longitude, marker.lat, marker.lon) }
                val title = marker.name

                MarkerCallout(
                    title = title,
                    subTitle = makeMarkerSubtitle(
                        latitude = marker.lat,
                        longitude = marker.lon,
                        distanceInMeters = distance
                    ),
                    shouldAnimate,
                    onAnimationDone = { shouldAnimate = false },
                    onEditAction = {
                        onMarkerEdit(marker, mapId)
                    },
                    onDeleteAction = {
                        mapState.removeCallout(calloutId)
                        mapState.removeMarker(markerState.idOnMap)
                        markerInteractor.deleteMarker(marker, mapId)
                    },
                    onMoveAction = {
                        mapState.removeCallout(calloutId)
                        morphToDynamic(markerState, x, y, mapState)
                    },
                    onStartItinerary = {
                        onStartItinerary(marker)
                    }
                )
            }
        }
        return true
    }

    private fun attachMarkerGrab(
        markerId: String,
        xMarker: Double,
        yMarker: Double,
        mapState: MapState,
        markerState: MarkerState
    ) {
        val grabId = "$markerGrabPrefix-$markerId"
        mapState.addMarker(grabId, xMarker, yMarker, Offset(-0.5f, -0.5f), zIndex = 0f) {
            MarkerGrab(
                morphedIn = !markerState.isStatic,
                onMorphOutDone = {
                    mapState.removeMarker(grabId)
                }
            )
        }
        mapState.enableMarkerDrag(grabId) { _, x, y, dx, dy, _, _ ->
            mapState.moveMarker(grabId, x + dx, y + dy)
            mapState.moveMarker(markerId, x + dx, y + dy)
        }
    }

    private fun onMarkerGrabTap(markerGrabId: String, mapState: MapState) {
        val markerId = markerGrabId.substringAfter("$markerPrefix-")
        val markerState = markerListState[markerId] ?: return
        markerState.isStatic = true

        mapState.updateMarkerClickable(markerState.idOnMap, true)

        val markerInfo = mapState.getMarkerInfo(markerState.idOnMap) ?: return
        val marker = markerState.marker
        scope.launch {
            dataStateFlow.firstOrNull()?.also {
                markerInteractor.addMarkerAtPosition(
                    marker,
                    it.map,
                    markerInfo.x,
                    markerInfo.y
                )
            }
        }
    }

    private fun addMarkerOnMap(
        marker: Marker,
        mapState: MapState,
        x: Double,
        y: Double
    ): MarkerState {
        val id = makeId(marker)
        val state = MarkerState(id, marker)

        mapState.addMarker(
            id,
            x,
            y,
            relativeOffset = Offset(-0.5f, -0.5f),
            zIndex = 1f,
            clickableAreaCenterOffset = Offset(0f, -0.25f),
            clickableAreaScale = Offset(2f, 1f)  // 48dp wide and height
        ) {
            val backgroundColor = parseColor(state.marker.color)
            val strokeColor = darkenColor(backgroundColor, 0.15f)

            Marker(
                isStatic = state.isStatic,
                backgroundColor = Color(backgroundColor),
                strokeColor = Color(strokeColor)
            )
        }
        return state
    }

    private fun morphToDynamic(markerState: MarkerState, x: Double, y: Double, mapState: MapState) {
        mapState.updateMarkerClickable(markerState.idOnMap, false)
        markerState.isStatic = false
        attachMarkerGrab(markerState.idOnMap, x, y, mapState, markerState)
    }
}

private fun makeId(marker: Marker): String = "$markerPrefix-${marker.id}"

private const val markerPrefix = "marker"
private const val calloutPrefix = "callout"
private const val markerGrabPrefix = "grabMarker"

private class MarkerState(val idOnMap: String, initMarker: Marker) {
    var marker by mutableStateOf<Marker>(initMarker)
    var isStatic by mutableStateOf(true)
}