package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.ui.graphics.Color
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.Route
import com.peterlaurence.trekme.features.map.domain.interactors.MapInteractor
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.util.parseColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ovh.plrapps.mapcompose.api.addPath
import ovh.plrapps.mapcompose.api.makePathDataBuilder
import ovh.plrapps.mapcompose.api.removePath
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.state.MapState
import java.util.*

class RouteLayer(
    private val scope: CoroutineScope,
    private val dataStateFlow: Flow<DataState>,
    mapFeatureEvents: MapFeatureEvents,
    private val mapInteractor: MapInteractor
) {
    private val staticRoutesData = mutableListOf<RouteData>()

    init {
        dataStateFlow.map { (map, mapState) ->
            drawStaticRoutes(mapState, map)
        }.launchIn(scope)

        mapFeatureEvents.trackVisibilityChanged.map { (mapId, route) ->
            onTrackVisibilityChanged(mapId, route)
        }.launchIn(scope)
    }

    private fun onTrackVisibilityChanged(mapId: Int, route: Route) = scope.launch {
        val (map, mapState) = dataStateFlow.first()
        if (map.id != mapId) return@launch

        if (route.visible) {
            val routeData = staticRoutesData.firstOrNull {
                it.route.id == route.id
            }.let { routeData ->
                routeData ?: withContext(Dispatchers.Default) {
                    makePathData(map, route, mapState)?.let {
                        RouteData(route, it)
                    }
                }
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

    private suspend fun drawStaticRoutes(mapState: MapState, map: Map) {
        staticRoutesData.clear()
        mapInteractor.loadRoutes(map)
        val routes = map.routes ?: return
        routes.asFlow().mapNotNull { route ->
            if (route.visible) {
                flow {
                    makePathData(map, route, mapState)?.let {
                        emit(RouteData(route, it))
                    }
                }.flowOn(Dispatchers.Default)
            } else null
        }.flattenMerge(4).collect {
            staticRoutesData.add(it)
            mapState.addPath(it)
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
            color = routeData.route.color?.let { colorStr ->
                Color(parseColor(colorStr))
            }
        )
    }

    private data class RouteData(val route: Route, val pathData: PathData)
}