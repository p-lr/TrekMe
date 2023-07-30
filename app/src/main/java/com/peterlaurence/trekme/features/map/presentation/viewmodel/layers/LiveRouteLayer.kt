package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.ui.graphics.Color
import com.peterlaurence.trekme.core.georecord.data.mapper.toMarker
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.events.recording.GpxRecordEvents
import com.peterlaurence.trekme.events.recording.LiveRoutePause
import com.peterlaurence.trekme.events.recording.LiveRoutePoint
import com.peterlaurence.trekme.events.recording.LiveRouteStop
import com.peterlaurence.trekme.features.map.domain.interactors.RouteInteractor
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.util.parseColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.state.MapState

class LiveRouteLayer(
    scope: CoroutineScope,
    private val dataStateFlow: Flow<DataState>,
    private val routeInteractor: RouteInteractor,
    private val gpxRecordEvents: GpxRecordEvents
) {
    private val colorLiveRoute = "#FF9800"

    init {
        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                drawLiveRoute(mapState, map)
            }
        }
    }

    private suspend fun drawLiveRoute(mapState: MapState, map: Map): Nothing = coroutineScope {
        val routeList = mutableListOf<Route>()

        fun newRoute(): Route {
            val route = Route(initialColor = colorLiveRoute)
            routeList.add(route)

            launch {
                val pathBuilder = mapState.makePathDataBuilder()
                routeInteractor.getLiveMarkerPositions(map, route).collect {
                    pathBuilder.addPoint(it.x, it.y)
                    val pathData = pathBuilder.build()
                    if (pathData != null) {
                        if (mapState.hasPath(route.id)) {
                            mapState.updatePath(route.id, pathData = pathData, count = pathData.size)
                        } else {
                            addPath(mapState, route, pathData)
                        }
                    }
                }
            }
            return route
        }

        var route = newRoute()

        gpxRecordEvents.liveRouteFlow.collect {
            when (it) {
                is LiveRoutePoint -> {
                    route.addMarker(it.pt.toMarker())
                }
                LiveRouteStop -> {
                    routeList.forEach { route ->
                        mapState.removePath(route.id)
                    }
                    routeList.clear()
                    route = newRoute()
                }
                LiveRoutePause -> {
                    /* Add previous route */
                    routeList.add(route)

                    /* Create and add a new route */
                    route = newRoute()
                }
            }
        }
    }

    private fun addPath(mapState: MapState, route: Route, pathData: PathData) {
        mapState.addPath(
            route.id,
            pathData,
            color = route.color.value.let { colorStr ->
                Color(parseColor(colorStr))
            }
        )
    }
}