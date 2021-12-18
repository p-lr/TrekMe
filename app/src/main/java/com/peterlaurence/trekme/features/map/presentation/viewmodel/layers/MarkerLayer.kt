package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

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
import com.peterlaurence.trekme.core.map.domain.Marker
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.features.map.presentation.ui.components.Marker
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerCallout
import com.peterlaurence.trekme.features.map.presentation.ui.components.MarkerGrab
import com.peterlaurence.trekme.features.map.presentation.viewmodel.LayerData
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapUiState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapViewModel
import com.peterlaurence.trekme.features.map.presentation.viewmodel.controllers.positionCallout
import com.peterlaurence.trekme.util.dpToPx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.*

class MarkerLayer(
    private val scope: CoroutineScope,
    private val layerData: Flow<LayerData>,
    markerMovedEvent: Flow<MapFeatureEvents.MarkerMovedEvent>,
    private val mapInteractor: MapInteractor,
    private val onMarkerEdit: (Marker, Int, String) -> Unit
): MapViewModel.MarkerTapListener {
    private var markerListState = mutableMapOf<String, MarkerState>()

    init {
        layerData.map {
            onMapUpdate(it.map, it.mapUiState)
        }.launchIn(scope)

        markerMovedEvent.combine(layerData) { event, it ->
            onMarkerUpdate(it.map, event.marker, event.markerId, it.mapUiState)
        }.launchIn(scope)
    }

    fun addMarker() {
        layerData.map { (map, mapUiState) ->
            val x = mapUiState.mapState.centroidX
            val y = mapUiState.mapState.centroidY
            val marker = mapInteractor.addMarker(map, x, y)
            val markerState = addMarkerOnMap(marker, mapUiState, x, y)
            morphToDynamic(markerState, x, y, mapUiState.mapState)
            markerListState[markerState.id] = markerState
        }.launchIn(scope)
    }

    private suspend fun onMapUpdate(map: Map, mapUiState: MapUiState) {
        mapInteractor.getMarkerPositions(map).map { (marker, x, y) ->
            val state = addMarkerOnMap(marker, mapUiState, x, y)
            state
        }.associateBy { it.id }.also {
            markerListState = it.toMutableMap()
        }
    }

    private suspend fun onMarkerUpdate(map: Map, marker: Marker, markerId: String, mapUiState: MapUiState) {
        val pos = mapInteractor.getMarkerPosition(map, marker) ?: return
        mapUiState.mapState.moveMarker(markerId, pos.x, pos.y)
    }

    override fun onMarkerTap(mapState: MapState, mapId: Int, id: String, x: Double, y: Double) {
        if (id.startsWith(markerGrabPrefix)) {
            onMarkerGrabTap(id, mapState)
            return
        }
        val markerState = markerListState[id] ?: return

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

            val calloutId = "$calloutPrefix-$id"
            mapState.addCallout(
                calloutId, x, y,
                relativeOffset = Offset(pos.relativeAnchorLeft, pos.relativeAnchorTop),
                absoluteOffset = Offset(pos.absoluteAnchorLeft, pos.absoluteAnchorTop),
                autoDismiss = true, clickable = false, zIndex = 3f
            ) {
                val marker = markerListState[id]?.marker ?: return@addCallout
                val subTitle = marker.let {
                    "${stringResource(id = R.string.latitude_short)} : ${df.format(it.lat)}  " +
                            "${stringResource(id = R.string.longitude_short)} : ${df.format(it.lon)}"
                }
                val title = marker.name ?: ""

                MarkerCallout(
                    DpSize(markerCalloutWidthDp.dp, markerCalloutHeightDp.dp),
                    title = title,
                    subTitle = subTitle,
                    shouldAnimate,
                    onAnimationDone = { shouldAnimate = false },
                    onEditAction = {
                        onMarkerEdit(marker, mapId, id)
                    },
                    onDeleteAction = {
                        mapState.removeCallout(calloutId)
                        mapState.removeMarker(id)
                        // TODO: implement real deletion, same for landmarks
                    },
                    onMoveAction = {
                        mapState.removeCallout(calloutId)
                        morphToDynamic(markerState, x, y, mapState)
                    }
                )
            }
        }
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
        mapState.enableMarkerDrag(grabId) { _, x, y, dx, dy ->
            mapState.moveMarker(grabId, x + dx, y + dy)
            mapState.moveMarker(markerId, x + dx, y + dy)
        }
    }

    private fun onMarkerGrabTap(markerGrabId: String, mapState: MapState) {
        val markerId = markerGrabId.substringAfter('-')
        val markerState = markerListState[markerId] ?: return
        markerState.isStatic = true
        mapState.updateMarkerClickable(markerId, true)

        val markerInfo = mapState.getMarkerInfo(markerId)
        val marker = markerListState[markerId]?.marker
        if (markerInfo != null && marker != null) {
            scope.launch {
                layerData.collect {
                    mapInteractor.updateAndSaveMarker(
                        marker,
                        it.map,
                        markerInfo.x,
                        markerInfo.y
                    )
                }
            }
        }
    }

    private fun addMarkerOnMap(marker: Marker, mapUiState: MapUiState, x: Double, y: Double): MarkerState {
        val id = "$markerPrefix-${UUID.randomUUID()}"
        val state = MarkerState(id, marker)
        mapUiState.mapState.addMarker(
            id,
            x,
            y,
            relativeOffset = Offset(-0.5f, -0.5f),
            zIndex = 1f
        ) {
            Marker(modifier = Modifier.padding(5.dp), isStatic = state.isStatic)
        }
        return state
    }

    private fun morphToDynamic(markerState: MarkerState, x: Double, y: Double, mapState: MapState) {
        mapState.updateMarkerClickable(markerState.id, false)
        markerState.isStatic = false
        attachMarkerGrab(markerState.id, x, y, mapState, markerState)
    }
}

private const val markerPrefix = "marker"
private const val calloutPrefix = "callout"
private const val markerGrabPrefix = "grabMarker"

private const val markerCalloutWidthDp = 200
private const val markerCalloutHeightDp = 120

private val df = DecimalFormat("#.####").apply {
    roundingMode = RoundingMode.CEILING
}

private data class MarkerState(val id: String, val marker: Marker) {
    var isStatic by mutableStateOf(true)
}