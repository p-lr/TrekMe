package com.peterlaurence.trekme.features.map.presentation.viewmodel.layers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Barycenter
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.features.common.domain.interactors.MapExcursionInteractor
import com.peterlaurence.trekme.features.map.domain.interactors.ExcursionInteractor
import com.peterlaurence.trekme.features.map.domain.interactors.RouteInteractor
import com.peterlaurence.trekme.features.map.presentation.model.RouteData
import com.peterlaurence.trekme.features.map.presentation.viewmodel.DataState
import com.peterlaurence.trekme.features.map.presentation.viewmodel.controllers.DistanceOnRouteController
import com.peterlaurence.trekme.util.parseColor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.paths.PathData
import ovh.plrapps.mapcompose.ui.state.MapState
import kotlin.math.min
import kotlin.math.max

class RouteLayer(
    scope: CoroutineScope,
    private val dataStateFlow: Flow<DataState>,
    private val goToRouteFlow: Flow<Route>,
    private val goToExcursionFlow: Flow<ExcursionRef>,
    private val routeInteractor: RouteInteractor,
    private val excursionInteractor: ExcursionInteractor,
    private val mapExcursionInteractor: MapExcursionInteractor,
) {
    val isShowingDistanceOnTrack = MutableStateFlow(false)
    private val staticRoutesData = MutableStateFlow(emptyMap<Route, RouteData>())
    private val excursionRoutesData = MutableStateFlow(emptyMap<Route, RouteData>())

    private val routeDataFlow = combine(staticRoutesData, excursionRoutesData) { s, e -> s + e }

    init {
        scope.launch {
            dataStateFlow.collectLatest { (_, mapState) ->
                goToRouteFlow.collectLatest event@{ route ->
                    val routeData = staticRoutesData.value[route] ?: return@event
                    mapState.scrollToBoundingBox(routeData.boundingBox)
                }
            }
        }

        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                coroutineScope {
                    staticRoutesData.update { emptyMap() }
                    excursionRoutesData.update { emptyMap() }

                    launch {
                        routeInteractor.loadRoutes(map)
                        map.routes.collectLatest { routes ->
                            drawStaticRoutes(mapState, map, routes)
                        }
                    }

                    launch {
                        mapExcursionInteractor.importExcursions(map)
                        map.excursionRefs.collectLatest { refs ->
                            val routesForRef = excursionInteractor.loadRoutes(refs)
                            launch {
                                drawExcursionRoutes(mapState, map, routesForRef)
                            }
                            launch {
                                listenForGoToExcursionEvent(mapState, routesForRef)
                            }
                        }
                    }
                }
            }
        }

        scope.launch {
            dataStateFlow.collectLatest { (map, mapState) ->
                val state = DistanceOnRouteController.DistanceOnRouteControllerRestoreState(
                    mutableListOf()
                )
                isShowingDistanceOnTrack.collectLatest {
                    if (it) {
                        val controller = DistanceOnRouteController(
                            map,
                            mapState,
                            routeInteractor,
                            routeDataFlow,
                            state
                        )
                        controller.processNearestRoute()
                    }
                }
            }
        }
    }

    private suspend fun listenForGoToExcursionEvent(
        mapState: MapState,
        routesForRef: kotlin.collections.Map<ExcursionRef, List<Route>>
    ) {
        goToExcursionFlow.collectLatest event@{ ref ->
            val routes = routesForRef[ref] ?: return@event
            val boundingBox = routes.mapNotNull {
                excursionRoutesData.value[it]?.boundingBox
            }.reduceOrNull { acc, b ->
                acc + b
            } ?: return@event
            mapState.scrollToBoundingBox(boundingBox)
        }
    }

    private suspend fun MapState.scrollToBoundingBox(boundingBox: BoundingBox) {
        scrollTo(boundingBox, padding = Offset(0.2f, 0.2f))
    }

    fun toggleDistanceOnTrack() {
        isShowingDistanceOnTrack.value = !isShowingDistanceOnTrack.value
    }

    private suspend fun drawExcursionRoutes(
        mapState: MapState,
        map: Map,
        routesForRef: kotlin.collections.Map<ExcursionRef, List<Route>>
    ) = coroutineScope {
        /* First, remove routes which are no longer associated with an excursion */
        val routesToRemove = excursionRoutesData.value.keys.filter {
            it !in routesForRef.values.flatten()
        }
        routesToRemove.forEach {
            mapState.removePath(it.id)
        }
        excursionRoutesData.update {
            it.filterKeys { route -> route !in routesToRemove }
        }

        for (ref in routesForRef.keys) {
            processExcursion(ref, routesForRef.getOrDefault(ref, emptyList()), map, mapState)
        }
    }

    private fun CoroutineScope.processExcursion(
        ref: ExcursionRef,
        routes: List<Route>,
        map: Map,
        mapState: MapState
    ) {
        /* React to color change */
        launch {
            ref.color.collect { color ->
                routes.forEach { route ->
                    route.color.value = color
                    mapState.updatePath(
                        route.id,
                        color = Color(parseColor(color))
                    )
                }
            }
        }

        /* React to visibility change */
        launch {
            ref.visible.collect { visible ->
                routes.forEach { route ->
                    val existing = excursionRoutesData.value[route]
                    if (visible) {
                        /* Only make route data if it wasn't already processed, or previously removed
                         * after visibility set to false. */
                        val routeData = existing ?: makeRouteData(map, route, mapState)

                        if (routeData != null && !mapState.hasPath(route.id)) {
                            excursionRoutesData.update {
                                it.toMutableMap().apply {
                                    set(route, routeData)
                                }
                            }
                            addPath(mapState, route, routeData.pathData)
                        }
                    } else {
                        if (existing != null) {
                            excursionRoutesData.update {
                                it.toMutableMap().apply { remove(route) }
                            }
                            mapState.removePath(route.id)
                        }
                    }
                }
            }
        }
    }

    /**
     * Anytime this suspend fun is invoked, previous invocation should be cancelled because
     * [processRoute] starts flow collections which need to be cancelled.
     */
    private suspend fun drawStaticRoutes(mapState: MapState, map: Map, routes: List<Route>) {
        coroutineScope {
            /* First, remove routes which are no longer in the list */
            val routesToRemove = staticRoutesData.value.keys.filter {
                it !in routes
            }
            routesToRemove.forEach {
                mapState.removePath(it.id)
            }
            staticRoutesData.update {
                it.filterKeys { route -> route !in routesToRemove }
            }

            /* Then, process current routes */
            for (route in routes) {
                processRoute(route, map, mapState)
            }
        }
    }

    /**
     * Depending on the route's visibility and color, perform required update and/or processing.
     * Notably:
     * * A newly added route have it's [PathData] generated and the corresponding path
     * is added to the [MapState].
     * * A route which was previously added is not re-processed.
     * * When the visibility becomes false, the corresponding path is removed from the [MapState].
     * If the visibility is later set to true, the [PathData] is re-generated.
     */
    private fun CoroutineScope.processRoute(route: Route, map: Map, mapState: MapState) {
        /* React to color change */
        launch {
            route.color.collect { color ->
                mapState.updatePath(
                    route.id,
                    color = Color(parseColor(color))
                )
            }
        }

        /* React to visibility change */
        launch {
            route.visible.collect { visible ->
                val existing = staticRoutesData.value[route]
                if (visible) {
                    /* Only make route data if it wasn't already processed, or previously removed
                     * after visibility set to false. */
                    val routeData = existing ?: makeRouteData(map, route, mapState)

                    if (routeData != null && !mapState.hasPath(route.id)) {
                        staticRoutesData.update {
                            it.toMutableMap().apply {
                                set(route, routeData)
                            }
                        }
                        addPath(mapState, route, routeData.pathData)
                    }
                } else {
                    if (existing != null) {
                        staticRoutesData.update {
                            it.toMutableMap().apply { remove(route) }
                        }
                        mapState.removePath(route.id)
                    }
                }
            }
        }
    }

    private suspend fun makeRouteData(map: Map, route: Route, mapState: MapState): RouteData? {
        val pathBuilder = mapState.makePathDataBuilder()

        var sumX = 0.0
        var sumY = 0.0
        var xMin: Double? = null
        var xMax: Double? = null
        var yMin: Double? = null
        var yMax: Double? = null
        var size = 0
        routeInteractor.getExistingMarkerPositions(map, route).collect {
            pathBuilder.addPoint(it.x, it.y)
            xMin = it.x.coerceAtMost(xMin ?: it.x)
            xMax = it.x.coerceAtLeast(xMax ?: it.x)
            yMin = it.y.coerceAtMost(yMin ?: it.y)
            yMax = it.y.coerceAtLeast(yMax ?: it.y)
            sumX += it.x
            sumY += it.y
            size++
        }

        val pathData = pathBuilder.build()

        val barycenter = if (size > 0) {
            Barycenter(sumX / size, sumY / size)
        } else null


        val boundingBox = if (xMin != null && xMax != null && yMin != null && yMax != null) {
            BoundingBox(xLeft = xMin!!, xRight = xMax!!, yBottom = yMax!!, yTop = yMin!!)
        } else null

        return if (pathData != null && barycenter != null && boundingBox != null) {
            RouteData(pathData, barycenter, boundingBox)
        } else null
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

    private operator fun BoundingBox.plus(b: BoundingBox): BoundingBox {
        return BoundingBox(
            xLeft = min(xLeft, b.xLeft),
            yTop = min(yTop, b.yTop),
            xRight = max(xRight, b.xRight),
            yBottom = max(yBottom, b.yBottom)
        )
    }
}
