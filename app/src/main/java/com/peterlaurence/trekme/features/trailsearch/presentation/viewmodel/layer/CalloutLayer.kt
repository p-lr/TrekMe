package com.peterlaurence.trekme.features.trailsearch.presentation.viewmodel.layer

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.core.location.domain.model.LatLon
import com.peterlaurence.trekme.core.map.domain.interactors.Wgs84ToMercatorInteractor
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
import ovh.plrapps.mapcompose.ui.state.MapState

class CalloutLayer(
    scope: CoroutineScope,
    private val mapStateFlow: Flow<MapState>,
    private val wgs84ToMercatorInteractor: Wgs84ToMercatorInteractor,
) {
    private val cursorChannel = Channel<CursorData>(Channel.CONFLATED)
    private val cursorMarkerId = "cursor"
    private var distance by mutableDoubleStateOf(0.0)
    private var elevation by mutableDoubleStateOf(0.0)

    init {
        scope.launch {
            mapStateFlow.collectLatest { mapState ->
                for (data in cursorChannel) {
                    distance = data.distance
                    elevation = data.ele
                    val normalized = withContext(Dispatchers.Default) {
                        wgs84ToMercatorInteractor.getNormalized(data.latLon.lat, data.latLon.lon)
                    } ?: continue
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