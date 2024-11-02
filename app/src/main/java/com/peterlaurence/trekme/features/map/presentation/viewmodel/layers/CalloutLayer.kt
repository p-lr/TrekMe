package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.core.location.domain.model.LatLon
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.features.trailsearch.presentation.ui.component.Cursor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.addCallout
import ovh.plrapps.mapcompose.api.hasCallout
import ovh.plrapps.mapcompose.api.moveCallout

class CalloutLayer(
    scope: CoroutineScope,
    private val dataStateFlow: Flow<DataState>,
    private val mapInteractor: MapInteractor,
) {
    private val cursorChannel = Channel<CursorData>(Channel.CONFLATED)
    private val cursorMarkerId = "cursor"
    private var distance by mutableDoubleStateOf(0.0)
    private var elevation by mutableDoubleStateOf(0.0)

    init {
        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                for (data in cursorChannel) {
                    distance = data.distance
                    elevation = data.ele
                    val normalized = withContext(Dispatchers.Default) {
                        mapInteractor.getNormalizedCoordinates(
                            map,
                            data.latLon.lat,
                            data.latLon.lon
                        )
                    }
                    if (mapState.hasCallout(cursorMarkerId)) {
                        mapState.moveCallout(cursorMarkerId, normalized.x, normalized.y)
                    } else {
                        mapState.addCallout(
                            id = cursorMarkerId,
                            x = normalized.x,
                            y = normalized.y,
                            relativeOffset = Offset(-0.5f, -1f),
                            zIndex = 1f
                        ) {
                            Cursor(
                                modifier = Modifier.padding(bottom = 18.dp),
                                distance = distance,
                                elevation = elevation
                            )
                        }
                    }
                }
            }
        }
    }

    fun setCursor(latLon: LatLon, distance: Double, ele: Double) {
        cursorChannel.trySend(CursorData(latLon, distance, ele))
    }
}

private data class CursorData(val latLon: LatLon, val distance: Double, val ele: Double)