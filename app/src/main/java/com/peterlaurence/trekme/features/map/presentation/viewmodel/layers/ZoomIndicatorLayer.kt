package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.runtime.snapshotFlow
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.ln

class ZoomIndicatorLayer(
    scope: CoroutineScope,
    private val isShowingZoomIndicator: Flow<Boolean>,
    private val dataStateFlow: Flow<DataState>,
) {

    val zoom = MutableStateFlow<Double?>(null)

    init {
        scope.launch {
            isShowingZoomIndicator.collectLatest { isShowing ->
                if (!isShowing) return@collectLatest
                dataStateFlow.collectLatest { (map, mapState) ->
                    zoom.value = null
                    updateZoom(map, mapState)
                }
            }
        }
    }

    private suspend fun updateZoom(map: Map, mapState: MapState) {
        val maxLevel = map.creationData?.maxLevel ?: return

        val scaleFlow = snapshotFlow {
            mapState.scale
        }

        scaleFlow.collect { scale ->
            zoom.value = maxLevel + ln(scale) / ln(2.0)
        }
    }
}