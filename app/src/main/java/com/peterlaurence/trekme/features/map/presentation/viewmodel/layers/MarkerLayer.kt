package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Marker
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.ui.components.Marker
import com.peterlaurence.trekme.features.map.presentation.viewmodel.LayerData
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapUiState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.MapViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.*

class MarkerLayer(
    private val scope: CoroutineScope,
    private val layerData: Flow<LayerData>,
    private val mapInteractor: MapInteractor
): MapViewModel.MarkerTapListener {
    private var markerListState = mapOf<String, MarkerState>()

    init {
        layerData.map {
            onMapUpdate(it.map, it.mapUiState)
        }.launchIn(scope)
    }

    private suspend fun onMapUpdate(map: Map, mapUiState: MapUiState) {
        mapInteractor.getMarkerPositions(map).map { (marker, x, y) ->
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
            state
        }.associateBy { it.id }.also {
            markerListState = it
        }
    }

    override fun onMarkerTap(mapState: MapState, id: String, x: Double, y: Double) {
        //TODO("Not yet implemented")
    }
}

private const val markerPrefix = "marker"
private const val calloutPrefix = "callout"
private const val markerGrabPrefix = "grabMarker"

private data class MarkerState(val id: String, val marker: Marker) {
    var isStatic by mutableStateOf(true)
}