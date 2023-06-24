package com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel.layer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionSearchItem
import com.peterlaurence.trekme.core.map.domain.interactors.Wgs84ToMercatorInteractor
import com.peterlaurence.trekme.features.excursionsearch.presentation.ui.component.Cluster
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.ExperimentalClusteringApi
import ovh.plrapps.mapcompose.api.addClusterer
import ovh.plrapps.mapcompose.api.addMarker
import ovh.plrapps.mapcompose.api.onMarkerClick
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.ui.state.markers.model.RenderingStrategy

class MarkerLayer(
    scope: CoroutineScope,
    val excursionItemsFlow: Flow<List<ExcursionSearchItem>>,
    val mapStateFlow: Flow<MapState>,
    val wgs84ToMercatorInteractor: Wgs84ToMercatorInteractor,
    val onExcursionItemClick: (ExcursionSearchItem) -> Unit
) {
    init {
        scope.launch {
            mapStateFlow.collectLatest { mapState ->
                configureClustering(mapState)
                excursionItemsFlow.collect { items ->
                    configureClickListener(mapState, items)
                    renderMarkers(mapState, items)
                }
            }
        }
    }

    @OptIn(ExperimentalClusteringApi::class)
    private fun configureClustering(mapState: MapState) {
        /* Add a marker clusterer to manage markers. In this example, we use "default" for the id */
        mapState.addClusterer("default") { ids ->
            { Cluster(size = ids.size) }
        }
    }

    private fun configureClickListener(mapState: MapState, items: List<ExcursionSearchItem>) {
        mapState.onMarkerClick { id, _, _ ->
            val item = items.firstOrNull { it.id == id }
            if (item != null) {
                onExcursionItemClick(item)
            }
        }
    }

    @OptIn(ExperimentalClusteringApi::class)
    private suspend fun renderMarkers(
        mapState: MapState,
        items: List<ExcursionSearchItem>
    ) = runCatching {
        val itemsWithPos = items.asFlow().map {
            flow {
                val normalizedPos = withContext(Dispatchers.Default) {
                    wgs84ToMercatorInteractor.getNormalized(it.startLat, it.startLon)
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
                    clickableAreaScale = Offset(1.5f, 1.5f),
                    renderingStrategy = RenderingStrategy.Clustering("default"),
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