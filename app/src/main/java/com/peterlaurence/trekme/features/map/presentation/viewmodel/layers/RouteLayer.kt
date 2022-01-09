package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.ui.graphics.Color
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Route
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.ui.mapview.colorRoute
import com.peterlaurence.trekme.util.parseColor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.makePathDataBuilder
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.api.updatePath
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
        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                drawStaticRoutes(mapState, map)
            }
        }
    }

    fun onNewRoutes(mapId: Int, routes: List<Route>) = scope.launch {
        dataStateFlow.collectLatest { (map, mapState) ->
            if (mapId != map.id) return@collectLatest
            coroutineScope {
                for (route in routes) {
                    processRoute(route, map, mapState)
                }
            }
        }
    }

    private suspend fun drawStaticRoutes(mapState: MapState, map: Map) = coroutineScope {
        staticRoutesData.clear()
        mapInteractor.loadRoutes(map)
        val routes = map.routes ?: return@coroutineScope

        for (route in routes) {
            processRoute(route, map, mapState)
        }
    }

    private fun CoroutineScope.processRoute(route: Route, map: Map, mapState: MapState) {
        /* React to color change */
        launch(Dispatchers.Default) {
            route.color.collect { color ->
                mapState.updatePath(
                    route.id,
                    color = Color(parseColor(color ?: colorRoute))
                )
            }
        }

        /* React to visibility change */
        launch(Dispatchers.Default) {
            route.visible.collect { visible ->
                if (visible) {
                    val routeData = makePathData(map, route, mapState)?.let {
                        RouteData(route, it)
                    }

                    if (routeData != null) {
                        staticRoutesData.add(routeData)
                        mapState.addPath(routeData)
                    }
                } else {
                    val existing = staticRoutesData.firstOrNull {
                        it.route.id == route.id
                    }
                    if (existing != null) {
                        staticRoutesData.remove(existing)
                        mapState.removePath(existing.route.id)
                    }
                }
            }
        }
    }

    private suspend fun makePathData(map: Map, route: Route, mapState: MapState): PathData? {
        val pathBuilder = mapState.makePathDataBuilder()
        mapInteractor.getRouteMarkerPositions(map, route).collect {
            pathBuilder.addPoint(it.x, it.y)
        }
        return pathBuilder.build()
    }

    private fun MapState.addPath(routeData: RouteData) {
        addPath(
            routeData.route.id,
            routeData.pathData,
            color = routeData.route.color.value.let { colorStr ->
                Color(parseColor(colorStr ?: colorRoute))
            }
        )
    }

    private data class RouteData(val route: Route, val pathData: PathData)
}