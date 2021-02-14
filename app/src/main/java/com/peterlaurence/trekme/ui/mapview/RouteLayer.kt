package com.peterlaurence.trekme.ui.mapview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Color
import android.graphics.Paint
import android.os.Parcelable
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.peterlaurence.mapview.MapView
import com.peterlaurence.mapview.ReferentialData
import com.peterlaurence.mapview.ReferentialOwner
import com.peterlaurence.mapview.api.addMarker
import com.peterlaurence.mapview.api.moveMarker
import com.peterlaurence.mapview.api.removeMarker
import com.peterlaurence.mapview.paths.PathView
import com.peterlaurence.mapview.paths.addPathView
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.getRelativeX
import com.peterlaurence.trekme.core.map.getRelativeY
import com.peterlaurence.trekme.core.map.gson.RouteGson
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.map.route.Barycenter
import com.peterlaurence.trekme.core.map.route.NearestMarkerCalculator
import com.peterlaurence.trekme.core.track.DistanceCalculatorImpl
import com.peterlaurence.trekme.core.units.UnitFormatter.formatDistance
import com.peterlaurence.trekme.ui.mapview.components.MarkerGrab
import com.peterlaurence.trekme.ui.mapview.components.tracksmanage.TracksManageFragment
import com.peterlaurence.trekme.ui.tools.TouchMoveListener
import com.peterlaurence.trekme.util.px
import com.peterlaurence.trekme.viewmodel.mapview.LiveRoute
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.parcelize.Parcelize
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * All [RouteGson.Route] are managed here.
 * This object is intended to be used exclusively by the [MapViewFragment].
 * After being created, the method [init] has to be called.
 *
 * @param state The [RouteLayerState] to restore. If null, nothing is restored.
 *
 * @author P.Laurence on 13/05/17 -- Converted to Kotlin on 16/02/2019
 */
class RouteLayer(
        private val coroutineScope: CoroutineScope,
        private val state: RouteLayerState? = null,
        private val mapLoader: MapLoader
) : TracksManageFragment.TrackChangeListener, CoroutineScope by coroutineScope {
    private var isInitialized = false
    private lateinit var mapView: MapView
    private lateinit var map: Map
    private var pathView: PathView? = null
    private var previousRoutes: List<RouteGson.Route>? = null
    private val liveRouteView: PathView by lazy {
        val context = mapView.context
        val view = PathView(context)
        mapView.addPathView(view)
        view
    }

    /**
     * This object can, in some circumstances, be accessed before the [init] method is invoked.
     * It happens when the application process is killed when the app is in the background in stopped
     * state, and Android reclaims memory. If the last destination was e.g the MapViewFragment, then
     * Android sometimes restores this "state" by immediately navigating to this fragment after the
     * main activity is re-created. However, if TrekMe is parametrized to display the map list upon
     * activity first start (the default), then the navigation framework is instructed to navigate
     * to the map list fragment shortly after. At that moment, the MapViewFragment (which has just
     * been created), might not be fully constructed and in particular this [RouteLayer] isn't
     * initialized yet. Navigating to the map list fragment while the current destination is the map
     * view fragment causes the latter fragment to be destroyed. Upon map view fragment destruction,
     * this layer is "destroyed" and [distanceOnRouteController] is accessed (see [destroy] method).
     *
     * This is the reason why accessing this object contains a fool-proof logic - [pathView] is only
     * initialized during [init].
     * P.S: A nice workaround to directly navigate back to the map view even when the process was
     * killed is to set the last map as the default destination (in the settings of TrekMe).
     */
    private var distanceOnRouteController: DistanceOnRouteController? = null
        get() {
            return field ?: pathView?.let { pathView ->
                field = DistanceOnRouteController(pathView, mapView, coroutineScope)
                field
            }
        }

    var isDistanceOnTrackActive: Boolean = false
        /* Report to be active if is either effectively active or is going to be */
        get() = isInitialized && (distanceOnRouteController?.isActive ?: false || state?.wasDistanceOnTrackActive ?: false)
        private set

    /**
     * Returns the state of this [RouteLayer] at the time of this method invocation.
     */
    fun getState(): RouteLayerState? {
        val controller = distanceOnRouteController ?: return null
        if (!isInitialized) return null
        return RouteLayerState(controller.getState(), controller.isActive)
    }

    /**
     * When a track file has been parsed, this method is called. At this stage, the new
     * [RouteGson.Route] are added to the [Map].
     *
     * @param map       the [Map] associated with the change
     * @param routeList a list of [RouteGson.Route]
     */
    override fun onTrackChanged(map: Map, routeList: List<RouteGson.Route>) {
        Log.d(TAG, routeList.size.toString() + " new route received for map " + map.name)

        /* At this stage, all routes might have already been drawn */
        if (previousRoutes?.containsAll(routeList) == false) drawStaticRoutes()
    }

    override fun onTrackVisibilityChanged() {
        pathView?.invalidate()
    }

    /**
     * This must be called when the [MapViewFragment] is ready to update its UI.
     */
    fun init(map: Map, mapView: MapView) {
        this.map = map
        setMapView(mapView)
        createPathView()
        isInitialized = true

        if (this.map.areRoutesDefined()) {
            drawStaticRoutes()
        } else {
            acquireThenDrawRoutes(this.map)
        }
    }

    fun destroy() {
        distanceOnRouteController?.destroy()
    }

    fun activateDistanceOnTrack() {
        val controller = distanceOnRouteController ?: return
        if (isInitialized && !controller.isActive) {
            mapView.addReferentialOwner(controller)
            controller.enable()
        }
    }

    fun disableDistanceOnTrack() {
        val controller = distanceOnRouteController ?: return
        if (!isInitialized) return
        mapView.removeReferentialOwner(controller)
        controller.disable()
    }

    private fun createPathView() {
        val view = PathView(mapView.context)
        mapView.addPathView(view)
        pathView = view
    }

    /**
     * The "live" route will be re-drawn completely, using the same pattern as static routes.
     */
    fun drawLiveRoute(liveRoute: LiveRoute) {
        /* Beware, don't make this a class attribute or the flow below will keep a reference on
         * the RouteLayer instance and consequently also the MapView. */
        val liveRoutePaint = Paint().apply {
            this.color = Color.parseColor(colorLiveRoute)
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

        coroutineScope.launch {
            liveRouteFlow.collect { liveRoute ->
                val paths = listOf(liveRoute.data as PathView.DrawablePath)
                liveRouteView.updatePaths(paths)
            }
        }
    }

    private fun CoroutineScope.acquireThenDrawRoutes(map: Map) = launch {
        /* Fetch and set routes to the map */
        mapLoader.importRoutesForMap(map)

        /* Then draw them */
        drawStaticRoutes()
    }

    /**
     * For each route, make a [PathView.DrawablePath] out of the path given by the flow.
     * Then, on the UI thread, render all static routes.
     */
    private fun drawStaticRoutes() {
        val routes = map.routes ?: return
        previousRoutes = routes
        val staticRouteFlow = getRouteFlow(routes) { route, path ->
            /* Honor the route color, if set */
            val paint = runCatching {
                Paint().apply {
                    color = Color.parseColor(route.color ?: colorRoute)
                }
            }.getOrNull()

            val drawablePath = object : PathView.DrawablePath {
                override val visible: Boolean
                    get() = route.visible
                override var path: FloatArray = path
                override var paint: Paint? = paint
                override val width: Float? = null
            }
            route.data = drawablePath
            route
        }

        coroutineScope.launch {
            val processedStaticRoutes = mutableListOf<RouteGson.Route>()
            staticRouteFlow.collect {
                processedStaticRoutes.add(it)
                pathView?.updatePaths(processedStaticRoutes.map { route ->
                    route.data as PathView.DrawablePath
                })
            }

            // TODO: The distanceOnRouteController is eagerly initialized here. It should instead be
            // created only when needed (although requires careful state management).
            /* Static routes are also used inside the DistanceOnRouteController.
             * To ensure proper rendering, we also restore the previous state (if any). */
            val controller = distanceOnRouteController ?: return@launch
            controller.setRoutes(processedStaticRoutes, map, state?.distOnRouteState)
            if (state != null && state.wasDistanceOnTrackActive && !controller.isActive) {
                mapView.addReferentialOwner(controller)
                controller.enable()
            }
        }
    }

    /**
     * Returns a [Flow] which concurrently compute [PathView.DrawablePath] for each route.
     */
    private fun getRouteFlow(routeList: List<RouteGson.Route>,
                             action: (RouteGson.Route, FloatArray) -> RouteGson.Route): Flow<RouteGson.Route> {
        return routeList.asFlow().map { route ->
            flow {
                val path = route.toPath(mapView)
                if (path != null) {
                    emit(action(route, path))
                }
            }.flowOn(Dispatchers.Default)
        }.flattenMerge(4)
    }

    private fun setMapView(mapView: MapView) {
        this.mapView = mapView
    }

    /**
     * Convert a [RouteGson.Route] to a [FloatArray] which is the drawable data structure expected
     * by the view that will represent it.
     */
    private fun RouteGson.Route.toPath(mapView: MapView): FloatArray? {
        val markerList = routeMarkers ?: listOf()
        /* If there is only one marker, the path has no sense */
        if (markerList.size < 2) return null

        val size = markerList.size * 4 - 4
        val lines = FloatArray(size)

        var i = 0
        var init = true

        /* While we iterate the list of markers, some new markers may be concurrently added to the
         * list. This is the case for the liveroute, inside the associated view-model.
         * By holding the monitor of the list of markers, we ensure that no new markers are added
         * meanwhile (as adding new markers requires the acquisition of the same lock). */
        synchronized(this) {
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
        }

        return lines
    }
}

/**
 * Contains all the logic related to the "distance on track" feature.
 * Public high-level methods are [enable] and [disable]. When the list of routes changes, the
 * [setRoutes] method should be used.
 *
 * @author P.Laurence on 19/05/2020
 */
private class DistanceOnRouteController(private val pathView: PathView,
                                        private val mapView: MapView,
                                        private val scope: CoroutineScope) : ReferentialOwner {
    var isActive: Boolean = false
        private set
    private var map: Map? = null
    private var routes: List<RouteGson.Route> = listOf()
    private var routeWithActiveDistance: RouteGson.Route? = null
    private var barycenterToRoute: kotlin.collections.Map<Barycenter, RouteGson.Route>? = null
    private val scrollUpdateChannel = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val distanceCalculateChannel = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val infoForRoute: MutableMap<RouteGson.Route, Info> = mutableMapOf()
    private val grab1 = MarkerGrab(mapView.context, 50.px)
    private val grab2 = MarkerGrab(mapView.context, 50.px)
    private var firstTouchMoveListener: TouchMoveListener? = null
    private var secondTouchMoveListener: TouchMoveListener? = null
    private var activeRouteLookupJob: Job? = null
    private var distanceCalculationJob: Job? = null
    private val distancePathWidth: Float
        get() {
            val metrics = pathView.resources.displayMetrics
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, metrics)
        }
    private val distMarker = TextView(mapView.context).apply {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setBackgroundResource(R.drawable.bordered_shape)
        setTextColor(Color.WHITE)
        setPadding(10, 0, 10, 0)
    }

    /**
     * The state is only the correspondence between each [RouteGson.Route]'s ids and the indexes of
     * the two [MarkerGrab]s.
     */
    fun getState(): DistOnRouteState {
        return infoForRoute.map {
            it.key.id to Pair(it.value.index1, it.value.index2)
        }.toMap()
    }

    override var referentialData: ReferentialData = ReferentialData(false, 0f, 1f, 0.0, 0.0)
        set(value) {
            field = value
            firstTouchMoveListener?.referentialData = value
            secondTouchMoveListener?.referentialData = value
            scrollUpdateChannel.tryEmit(Unit)
        }

    fun enable() {
        isActive = true
        activeRouteLookupJob = scope.launch {
            scrollUpdateChannel.sample(32).collect {
                updateActiveRoute()
            }
        }
        scrollUpdateChannel.tryEmit(Unit)  // Trigger the first computation

        distanceCalculationJob = scope.launch {
            distanceCalculateChannel.sample(32).collect {
                updateDistance()
            }
        }

        mapView.addMarker(distMarker, 0.0, 0.0, -0.5f, -0.5f)
        distMarker.visibility = View.GONE
    }

    fun disable() {
        isActive = false
        /* Cancel any ongoing operation */
        activeRouteLookupJob?.cancel()
        distanceCalculationJob?.cancel()
        routeWithActiveDistance = null

        /* Remove the grab markers */
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

        /* Remove the distance indicator */
        mapView.removeMarker(distMarker)

        /* Finally, restore original paths */
        val originalPaths = routes.filter { it.visible }.map {
            it.data as PathView.DrawablePath
        }
        pathView.updatePaths(originalPaths)
    }

    fun destroy() {
        activeRouteLookupJob?.cancel()
        distanceCalculationJob?.cancel()
    }

    /**
     * Update the internal list of routes along the former state, if any.
     * It immediately triggers an internal computation of each chunk's barycenter (off UI thread).
     * If the provided state is non-null, it restores relevant structures.
     */
    suspend fun setRoutes(routeList: List<RouteGson.Route>, map: Map, state: DistOnRouteState?) {
        this.map = map
        routes = routeList

        barycenterToRoute = routeList.map { route ->
            scope.async(Dispatchers.Default) {
                Pair(computeBarycenter(route, map), route)
            }
        }.awaitAll().toMap()

        /* Restore from the former state */
        state?.forEach { (id, pair) ->
            val route = routes.firstOrNull {
                it.id == id
            }
            if (route != null) {
                infoForRoute[route] = Info(pair.first, pair.second)
            }
        }
    }

    private fun computeBarycenter(route: RouteGson.Route, map: Map): Barycenter {
        var sumX = 0.0
        var sumY = 0.0
        for (point in route.routeMarkers) {
            sumX += point.getRelativeX(map)
            sumY += point.getRelativeY(map)
        }
        val size = route.routeMarkers.size
        return Barycenter(sumX / size, sumY / size)
    }

    private fun updateActiveRoute() {
        /* Compute the relative coordinates of the center of the MapView's visible area */
        val x = mapView.coordinateTranslater.translateAbsoluteToRelativeX((referentialData.centerX * mapView.coordinateTranslater.baseWidth).toInt())
        val y = mapView.coordinateTranslater.translateAbsoluteToRelativeY((referentialData.centerY * mapView.coordinateTranslater.baseHeight).toInt())

        barycenterToRoute?.filter { it.value.visible }?.minByOrNull {
            computeDistance(x, y, it.key)
        }?.also {
            /* Only if this is a different route, position the markers on this route */
            val map = map
            if (it.value != routeWithActiveDistance && map != null) {
                setRouteWithActiveDistance(it.value, map)
                render()

                /* And trigger the first distance calculation */
                distanceCalculateChannel.tryEmit(Unit)
            }
        }
    }

    private fun setRouteWithActiveDistance(route: RouteGson.Route, map: Map) {
        routeWithActiveDistance = route

        val info = infoForRoute[route]

        /* Animate the markers */
        grab1.morphIn()
        grab2.morphIn()

        val nearestMarkerCalculator = NearestMarkerCalculator(route, map)
        firstTouchMoveListener = TouchMoveListener(mapView) { mapView, view, x, y ->
            scope.launch {
                val markerIndexed = nearestMarkerCalculator.findNearest(x, y)
                if (markerIndexed != null && view != null) {
                    mapView?.moveMarker(view,
                            markerIndexed.marker.getRelativeX(map),
                            markerIndexed.marker.getRelativeY(map))
                    infoForRoute[route]?.index1 = markerIndexed.index
                    distanceCalculateChannel.tryEmit(Unit)
                    pathView.invalidate()
                }
            }
        }
        grab1.setOnTouchListener(firstTouchMoveListener)

        secondTouchMoveListener = TouchMoveListener(mapView) { mapView, view, x, y ->
            scope.launch {
                val markerIndexed = nearestMarkerCalculator.findNearest(x, y)
                if (markerIndexed != null && view != null) {
                    mapView?.moveMarker(view,
                            markerIndexed.marker.getRelativeX(map),
                            markerIndexed.marker.getRelativeY(map))
                    infoForRoute[route]?.index2 = markerIndexed.index
                    distanceCalculateChannel.tryEmit(Unit)
                    pathView.invalidate()
                }
            }
        }
        grab2.setOnTouchListener(secondTouchMoveListener)

        if (info == null) {
            /* This is the first time we are positioning markers - use default position */
            val index1 = 0
            val index2 = route.routeMarkers.size / 4
            infoForRoute[route] = Info(index1, index2)
            positionGrabMarkers(map, route, index1, index2)
        } else {
            /* The user is "navigating back" to this route - use remembered positions */
            positionGrabMarkers(map, route, info.index1, info.index2)
        }
    }

    private fun positionGrabMarkers(map: Map, route: RouteGson.Route, index1: Int, index2: Int) {
        val firstMarker = route.routeMarkers[index1]
        if (firstMarker != null) {
            val relX = firstMarker.getRelativeX(map)
            val relY = firstMarker.getRelativeY(map)
            if (grab1.parent == null) {
                mapView.addMarker(grab1, relX, relY, -0.5f, -0.5f)
            } else {
                mapView.moveMarker(grab1, relX, relY)
            }
        }

        val secondMarker = route.routeMarkers[index2]
        if (secondMarker != null) {
            val relX = secondMarker.getRelativeX(map)
            val relY = secondMarker.getRelativeY(map)
            if (grab2.parent == null) {
                mapView.addMarker(grab2, relX, relY, -0.5f, -0.5f)
            } else {
                mapView.moveMarker(grab2, relX, relY)
            }
        }
    }

    fun render() {
        val routeWithActiveDistance = routeWithActiveDistance ?: return
        val otherPaths = routes.filter { it != routeWithActiveDistance }.map {
            it.data as PathView.DrawablePath
        }

        val drawablePath = routeWithActiveDistance.data as PathView.DrawablePath
        val distancePaint = Paint().apply { color = Color.parseColor("#F50057") }

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

    private suspend fun updateDistance() {
        val activeRoute = routeWithActiveDistance ?: return
        val info = infoForRoute[activeRoute] ?: return
        val map = map ?: return

        val distance = computeDistance(activeRoute, info)
        distMarker.text = formatDistance(distance)
        distMarker.visibility = View.VISIBLE

        val firstMarker = activeRoute.routeMarkers[info.index1]
        val secondMarker = activeRoute.routeMarkers[info.index2]
        val x = (firstMarker.getRelativeX(map) + secondMarker.getRelativeX(map)) / 2
        val y = (firstMarker.getRelativeY(map) + secondMarker.getRelativeY(map)) / 2

        mapView.moveMarker(distMarker, x, y)
    }

    private suspend fun computeDistance(route: RouteGson.Route, info: Info): Double = withContext(Dispatchers.Default) {
        val iMin = min(info.index1, info.index2)
        val iMax = max(info.index1, info.index2)
        val iterator = route.routeMarkers.listIterator(iMin)

        val distanceCalculator = DistanceCalculatorImpl(route.elevationTrusted)
        for (i in iMin until iMax) {
            val marker = iterator.next()
            distanceCalculator.addPoint(marker.lat, marker.lon, marker.elevation)
        }

        distanceCalculator.getDistance()
    }

    private data class Info(var index1: Int, var index2: Int)
}

@Parcelize
class RouteLayerState(val distOnRouteState: DistOnRouteState,
                      val wasDistanceOnTrackActive: Boolean) : Parcelable

typealias DistOnRouteState = kotlin.collections.Map<Int, Pair<Int, Int>>

private const val TAG = "RouteLayer"