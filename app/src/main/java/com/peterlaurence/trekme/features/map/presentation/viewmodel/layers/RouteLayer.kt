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

    private suspend fun drawStaticRoutes(mapState: MapState, map: Map) = coroutineScope {
        staticRoutesData.clear()
        mapInteractor.loadRoutes(map)
        val routes = map.routes ?: return@coroutineScope

        for (route in routes) {
            launch(Dispatchers.Default) {
                combine(route.visible, route.color) { visible, color ->
                    if (visible) {
                        if (staticRoutesData.any { it.route == route }) {
                            mapState.updatePath(
                                route.id,
                                color = Color(parseColor(color ?: colorRoute))
                            )
                        } else {
                            val routeData = makePathData(map, route, mapState)?.let {
                                RouteData(route, it)
                            }

                            if (routeData != null) {
                                staticRoutesData.add(routeData)
                                mapState.addPath(routeData)
                            }
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
                }.collect()
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