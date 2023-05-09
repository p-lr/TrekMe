package com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.layer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionSearchItem
import com.peterlaurence.trekme.core.map.domain.interactors.Wgs84ToNormalizedInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.ui.state.MapState

class MarkerLayer(
    scope: CoroutineScope,
    val excursionItemsFlow: StateFlow<List<ExcursionSearchItem>>,
    val mapStateFlow: Flow<MapState>,
    val wgs84ToNormalizedInteractor: Wgs84ToNormalizedInteractor
) {

    init {
        scope.launch {
            mapStateFlow.collectLatest { mapState ->
                configure(mapState)
                excursionItemsFlow.collect { items ->
                    renderMarkers(mapState, items)
                }
            }
        }
    }

    private suspend fun configure(mapState: MapState) {
        mapState.onMarkerClick { id, x, y ->
            println("xxxxx marker click $id")
        }
    }

    private suspend fun renderMarkers(
        mapState: MapState,
        items: List<ExcursionSearchItem>
    ) = runCatching {
        val itemsWithPos = items.asFlow().map {
            flow {
                val normalizedPos = withContext(Dispatchers.Default) {
                    wgs84ToNormalizedInteractor.getNormalized(it.startLat, it.startLon)
                }
                emit(Pair(it, normalizedPos))
            }
        }.flattenMerge(concurrency = 4)

        itemsWithPos.collect { (item, pos) ->
            if (pos != null) {
                mapState.addMarker(
                    id = item.id,
                    x = pos.x,
                    y = pos.y,
                    clickableAreaScale = Offset(1.5f, 1.5f)
                ) {
                    Image(
                        modifier = Modifier.size(28.dp, 40.dp),
                        painter = painterResource(id = R.drawable.pin_hiking),
                        contentDescription = null
                    )
                }
            }
        }
    }
}