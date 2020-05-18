package com.peterlaurence.trekme.ui.mapview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.util.TypedValue
import com.peterlaurence.mapview.MapView
import com.peterlaurence.mapview.ReferentialData
import com.peterlaurence.mapview.ReferentialOwner
import com.peterlaurence.mapview.api.addMarker
import com.peterlaurence.mapview.api.moveMarker
import com.peterlaurence.mapview.api.removeMarker
import com.peterlaurence.mapview.paths.PathView
import com.peterlaurence.mapview.paths.addPathView
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.MarkerGson
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader.getRoutesForMap
import com.peterlaurence.trekme.core.map.route.Barycenter
import com.peterlaurence.trekme.core.map.route.NearestMarkerCalculator
import com.peterlaurence.trekme.ui.mapview.components.MarkerGrab
import com.peterlaurence.trekme.ui.mapview.components.tracksmanage.TracksManageFragment
import com.peterlaurence.trekme.ui.tools.TouchMoveListener
import com.peterlaurence.trekme.util.px
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * All [RouteGson.Route] are managed here.
 * This object is intended to be used exclusively by the [MapViewFragment].
 *
 * After being created, the method [init] has to be called.
 *
 * @author peterLaurence on 13/05/17 -- Converted to Kotlin on 16/02/2019
 */
class RouteLayer(private val coroutineScope: CoroutineScope) :
        TracksManageFragment.TrackChangeListener,
        CoroutineScope by coroutineScope {
    private lateinit var mapView: MapView
    private lateinit var map: Map
    private lateinit var pathView: PathView
    private val liveRouteView: PathView by lazy {
        val context = mapView.context
        val view = PathView(context)
        mapView.addPathView(view)
        view
    }
    private val processedStaticRoutes = mutableListOf<RouteGson.Route>()
    private val processedLiveRoute = mutableListOf<RouteGson.Route>()
    private val distanceOnRouteController by lazy {
        DistanceOnRouteController(pathView, mapView, coroutineScope)
    }

    var isDistanceOnTrackActive: Boolean = false
        private set

    /**
     * When a track file has been parsed, this method is called. At this stage, the new
     * [RouteGson.Route] are added to the [Map].
     *
     * @param map       the [Map] associated with the change
     * @param routeList a list of [RouteGson.Route]
     */
    override fun onTrackChanged(map: Map, routeList: List<RouteGson.Route>) {
        Log.d(TAG, routeList.size.toString() + " new route received for map " + map.name)

        drawStaticRoutes()
    }

    override fun onTrackVisibilityChanged() {
        if (::pathView.isInitialized) {
            pathView.invalidate()
        }
    }

    /**
     * This must be called when the [MapViewFragment] is ready to update its UI.
     */
    fun init(map: Map, mapView: MapView) {
        this.map = map
        setMapView(mapView)
        createPathView()

        if (this.map.areRoutesDefined()) {
            drawStaticRoutes()
            if (isDistanceOnTrackActive) activateDistanceOnTrack()
        } else {
            acquireThenDrawRoutes(this.map)
        }
    }

    fun activateDistanceOnTrack() {
        mapView.addReferentialOwner(distanceOnRouteController)
        distanceOnRouteController.enable()
    }

    fun disableDistanceOnTrack() {
        mapView.removeReferentialOwner(distanceOnRouteController)
        distanceOnRouteController.disable()
    }

    private fun createPathView() {
        pathView = PathView(mapView.context)
        mapView.addPathView(pathView)
    }

    /**
     * The "live" route will be re-drawn completely, using the same pattern as static routes.
     */
    fun drawLiveRoute(liveRoute: RouteGson.Route) {
        /* Beware, don't make this a class attribute or the flow below will keep a reference on
         * the RouteLayer instance and consequently also the MapView. */
        val liveRoutePaint = Paint().apply {
            this.color = Color.parseColor("#FF9800")
        }
        val liveRouteFlow = getRouteFlow(listOf(liveRoute)) { route, path ->
            val drawablePath = object : PathView.DrawablePath {
                override val visible: Boolean
                    get() = route.visible
                override var path: FloatArray = path
                override var paint: Paint? = liveRoutePaint
                override val width: Float? = null
            }
            route.data = drawablePath
            route
        }

        processedLiveRoute.clear()
        coroutineScope.launch {
            liveRouteFlow.collect {
                processedLiveRoute.add(it)
                liveRouteView.updatePaths(processedLiveRoute.map { route ->
                    route.data as PathView.DrawablePath
                })
            }
        }
    }

    private fun CoroutineScope.acquireThenDrawRoutes(map: Map) = launch {
        /* Fetch and set routes to the map */
        getRoutesForMap(map)

        /* Then draw them */
        drawStaticRoutes()
    }

    /**
     * For each route, make a [PathView.DrawablePath] out of the path given by the flow.
     * Then, on the UI thread, render all static routes.
     */
    private fun drawStaticRoutes() {
        val routes = map.routes ?: return
        val staticRouteFlow = getRouteFlow(routes) { route, path ->
            val drawablePath = object : PathView.DrawablePath {
                override val visible: Boolean
                    get() = route.visible
                override var path: FloatArray = path
                override var paint: Paint? = null
                override val width: Float? = null
            }
            route.data = drawablePath
            route
        }

        processedStaticRoutes.clear()
        coroutineScope.launch {
            staticRouteFlow.collect {
                processedStaticRoutes.add(it)
                pathView.updatePaths(processedStaticRoutes.map { route ->
                    route.data as PathView.DrawablePath
                })
            }

            distanceOnRouteController.setRoutes(processedStaticRoutes)
        }
    }

    /**
     * Returns a [Flow] which asynchronously compute [PathView.DrawablePath] for each route.
     */
    private fun getRouteFlow(routeList: List<RouteGson.Route>,
                             action: (RouteGson.Route, FloatArray) -> RouteGson.Route): Flow<RouteGson.Route> {
        return routeList.asFlow().mapNotNull { route ->
            val path = route.toPath(mapView)
            if (path != null) {
                action(route, path)
            } else null
        }.buffer().flowOn(Dispatchers.Default)
    }

    private fun setMapView(mapView: MapView) {
        this.mapView = mapView
    }

    /**
     * Convert a [RouteGson.Route] to a [FloatArray] which is the drawable data structure expected
     * by the view that will represent it.
     */
    private fun RouteGson.Route.toPath(mapView: MapView): FloatArray? {
        val markerList = route_markers ?: listOf()
        /* If there is only one marker, the path has no sense */
        if (markerList.size < 2) return null

        val size = markerList.size * 4 - 4
        val lines = FloatArray(size)

        var i = 0
        var init = true
        for (marker in markerList) {
            val relativeX = marker.getRelativeX(map)
            val relativeY = marker.getRelativeY(map)
            if (init) {
                lines[i] = mapView.coordinateTranslater.translateX(relativeX).toFloat()
                lines[i + 1] = mapView.coordinateTranslater.translateY(relativeY).toFloat()
                init = false
                i += 2
            } else {
                lines[i] = mapView.coordinateTranslater.translateX(relativeX).toFloat()
                lines[i + 1] = mapView.coordinateTranslater.translateY(relativeY).toFloat()
                if (i + 2 >= size) break
                lines[i + 2] = lines[i]
                lines[i + 3] = lines[i + 1]
                i += 4
            }
        }

        return lines
    }

    private fun MarkerGson.Marker.getRelativeX(map: Map): Double {
        val mapUsesProjection = map.projection != null
        return if (mapUsesProjection) proj_x else lon
    }

    private fun MarkerGson.Marker.getRelativeY(map: Map): Double {
        val mapUsesProjection = map.projection != null
        return if (mapUsesProjection) proj_y else lat
    }
}

private class DistanceOnRouteController(private val pathView: PathView,
                                        private val mapView: MapView,
                                        private val scope: CoroutineScope) : ReferentialOwner {
    private var routes: List<RouteGson.Route> = listOf()
    private var routeWithActiveDistance: RouteGson.Route? = null
    private var barycenterToRoute: kotlin.collections.Map<Barycenter, RouteGson.Route>? = null
    private var scrollUpdateChannel = ConflatedBroadcastChannel<Unit>()
    private val infoForRoute: MutableMap<RouteGson.Route, Info> = mutableMapOf()
    private val grab1 = MarkerGrab(mapView.context, 50.px)
    private val grab2 = MarkerGrab(mapView.context, 50.px)
    private var activeRouteLookupJob: Job? = null
    private val distancePathWidth: Float
        get() {
            val metrics = pathView.resources.displayMetrics
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, metrics)
        }

    override var referentialData: ReferentialData = ReferentialData(false, 0f, 1f, 0.0, 0.0)
        set(value) {
            field = value
            scrollUpdateChannel.offer(Unit)
        }

    fun enable() {
        activeRouteLookupJob = scope.launch {
            scrollUpdateChannel.asFlow().sample(32).collect {
                updateActiveRoute()
            }
        }
    }

    fun disable() {
        activeRouteLookupJob?.cancel()

        routeWithActiveDistance = null
        grab1.morphOut(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                mapView.removeMarker(grab1)
            }
        })
        grab2.morphOut(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                mapView.removeMarker(grab2)
            }
        })
    }

    fun setRoutes(routeList: List<RouteGson.Route>) {
        routes = routeList

        val baryCentersFlow = routeList.asFlow().map { route ->
            Pair(computeBarycenter(route), route)
        }.flowOn(Dispatchers.Default).buffer()

        scope.launch {
            barycenterToRoute = baryCentersFlow.toList().toMap()
        }
    }

    private fun computeBarycenter(route: RouteGson.Route): Barycenter {
        var sumX = 0.0
        var sumY = 0.0
        for (point in route.route_markers) {
            sumX += point.proj_x
            sumY += point.proj_y
        }
        val size = route.route_markers.size
        return Barycenter(sumX / size, sumY / size)
    }

    private fun updateActiveRoute() {
        /* Compute the relative coordinates of the center of the MapView's visible area */
        val x = mapView.coordinateTranslater.translateAbsoluteToRelativeX((referentialData.centerX * mapView.coordinateTranslater.baseWidth).toInt())
        val y = mapView.coordinateTranslater.translateAbsoluteToRelativeY((referentialData.centerY * mapView.coordinateTranslater.baseHeight).toInt())

        barycenterToRoute?.filter { it.value.visible }?.minBy {
            computeDistance(x, y, it.key)
        }?.also {
            /* Only if this is a different route, position the markers on this route */
            if (it.value != routeWithActiveDistance) {
                setRouteWithActiveDistance(it.value)
                render()
            }
        }
    }

    private fun setRouteWithActiveDistance(route: RouteGson.Route) {
        routeWithActiveDistance = route

        val info = infoForRoute[route]

        /* Animate the markers */
        grab1.morphIn()
        grab2.morphIn()

        val nearestMarkerCalculator = NearestMarkerCalculator(route)
        grab1.setOnTouchListener(TouchMoveListener(mapView,
                TouchMoveListener.MarkerMoveAgent { mapView, view, x, y ->
                    scope.launch {
                        val markerIndexed = nearestMarkerCalculator.findNearest(x, y)
                        if (markerIndexed != null && view != null) {
                            mapView?.moveMarker(view, markerIndexed.marker.proj_x, markerIndexed.marker.proj_y)
                            infoForRoute[route]?.index1 = markerIndexed.index
                            pathView.invalidate()
                        }
                    }
                })
        )

        grab2.setOnTouchListener(TouchMoveListener(mapView,
                TouchMoveListener.MarkerMoveAgent { mapView, view, x, y ->
                    scope.launch {
                        val markerIndexed = nearestMarkerCalculator.findNearest(x, y)
                        if (markerIndexed != null && view != null) {
                            mapView?.moveMarker(view, markerIndexed.marker.proj_x, markerIndexed.marker.proj_y)
                            infoForRoute[route]?.index2 = markerIndexed.index
                            pathView.invalidate()
                        }
                    }
                })
        )

        if (info == null) {
            /* This is the first time we are positioning markers - use default position */
            val index1 = 0
            val index2 = route.route_markers.size / 4
            infoForRoute[route] = Info(index1, index2)
            positionGrabMarkers(route, index1, index2)
        } else {
            /* The user is "navigating back" to this route - use remembered positions */
            positionGrabMarkers(route, info.index1, info.index2)
        }
    }

    private fun positionGrabMarkers(route: RouteGson.Route, index1: Int, index2: Int) {
        val firstMarker = route.route_markers[index1]
        if (firstMarker != null) {
            if (grab1.parent == null) {
                mapView.addMarker(grab1, firstMarker.proj_x, firstMarker.proj_y, -0.5f, -0.5f)
            } else {
                mapView.moveMarker(grab1, firstMarker.proj_x, firstMarker.proj_y)
            }
        }

        val secondMarker = route.route_markers[index2]
        if (secondMarker != null) {
            if (grab2.parent == null) {
                mapView.addMarker(grab2, secondMarker.proj_x, secondMarker.proj_y, -0.5f, -0.5f)
            } else {
                mapView.moveMarker(grab2, secondMarker.proj_x, secondMarker.proj_y)
            }
        }
    }

    fun render() {
        val routeWithActiveDistance = routeWithActiveDistance ?: return
        val otherPaths = routes.filter { it != routeWithActiveDistance }.map {
            it.data as PathView.DrawablePath
        }

        val drawablePath = routeWithActiveDistance.data as PathView.DrawablePath
        val distancePaint = Paint().apply { color = Color.MAGENTA }

        /* Since each index in the route takes 4 numbers in the FloatArray, indexes are
         * multiplied by 4 */
        fun i1(): Int = 4 * (infoForRoute[routeWithActiveDistance]?.index1 ?: 0)
        fun i2(): Int = 4 * (infoForRoute[routeWithActiveDistance]?.index2 ?: 0)
        val length = drawablePath.path.size

        val distancePath = object : PathView.DrawablePath {
            override val visible: Boolean
                get() = routeWithActiveDistance.visible
            override var path: FloatArray = drawablePath.path
            override val width: Float? = distancePathWidth
            override var paint: Paint? = distancePaint
            override val count: Int
                get() = abs(i2() - i1()).coerceAtMost(length)

            override val offset: Int
                get() = min(i1(), i2()).coerceAtMost(length)
        }

        /* The part between the start of the original path and the start of the distance path */
        val beforePath = object : PathView.DrawablePath {
            override val visible: Boolean
                get() = routeWithActiveDistance.visible
            override var path: FloatArray = drawablePath.path
            override val width: Float? = null
            override var paint: Paint? = null
            override val count: Int
                get() = min(i1(), i2()).coerceAtMost(length)

            override val offset: Int = 0
        }

        /* The part between the end of the distance path and the end of the original path */
        val afterPath = object : PathView.DrawablePath {
            override val visible: Boolean
                get() = routeWithActiveDistance.visible
            override var path: FloatArray = drawablePath.path
            override val width: Float? = null
            override var paint: Paint? = null
            override val count: Int
                get() = (drawablePath.path.size - max(i1(), i2())).coerceAtLeast(0)

            override val offset: Int
                get() = max(i1(), i2()).coerceAtMost(length)
        }

        pathView.updatePaths(otherPaths + beforePath + distancePath + afterPath)
    }

    private fun computeDistance(x: Double, y: Double, barycenter: Barycenter): Double {
        return (barycenter.x - x).pow(2) + (barycenter.y - y).pow(2)
    }

    private data class Info(var index1: Int, var index2: Int)
}

private const val TAG = "RouteLayer"