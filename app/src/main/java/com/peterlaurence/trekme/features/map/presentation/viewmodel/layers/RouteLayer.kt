package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.ui.graphics.Color
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Route
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.util.parseColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.makePathDataBuilder
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.*

class RouteLayer(
    private val scope: CoroutineScope,
    private val dataStateFlow: Flow<DataState>,
    private val mapInteractor: MapInteractor
) {
    private val staticRoutesData = mutableListOf<RouteData>()

    init {
        dataStateFlow.map { (map, mapState) ->
            drawStaticRoutes(mapState, map)
        }.launchIn(scope)
    }

    private suspend fun drawStaticRoutes(mapState: MapState, map: Map) {
        staticRoutesData.clear()
        mapInteractor.loadRoutes(map)
        val routes = map.routes ?: return
        routes.asFlow().mapNotNull { route ->
            if (route.visible) {
                flow {
                    val pathBuilder = mapState.makePathDataBuilder()
                    mapInteractor.getRouteMarkerPositions(map, route).collect {
                        pathBuilder.addPoint(it.x, it.y)
                    }
                    pathBuilder.build()?.let {
                        emit(RouteData(route, it))
                    }
                }.flowOn(Dispatchers.Default)
            } else null
        }.flattenMerge(4).collect {
            staticRoutesData.add(it)
            mapState.addPath(
                it.id,
                it.pathData,
                color = it.route.color?.let { colorStr ->
                    Color(parseColor(colorStr))
                }
            )
        }
    }

    private data class RouteData(val route: Route, val pathData: PathData) {
        val id = UUID.randomUUID().toString()
    }
}