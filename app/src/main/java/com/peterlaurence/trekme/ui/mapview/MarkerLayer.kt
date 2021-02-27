package com.peterlaurence.trekme.ui.mapview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.View
import androidx.navigation.Navigation
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.MarkerGson.Marker
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.ui.mapview.components.MarkerCallout
import com.peterlaurence.trekme.ui.mapview.components.MarkerGrab
import com.peterlaurence.trekme.ui.mapview.components.MovableMarker
import com.peterlaurence.trekme.ui.mapview.controller.positionCallout
import com.peterlaurence.trekme.ui.tools.TouchMoveListener
import com.peterlaurence.trekme.ui.tools.TouchMoveListener.ClickCallback
import com.peterlaurence.trekme.ui.tools.TouchMoveListener.MarkerMoveAgent
import com.peterlaurence.trekme.util.px
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapview.MapView
import ovh.plrapps.mapview.api.*
import ovh.plrapps.mapview.markers.MarkerTapListener

/**
 * All [MovableMarker] and [MarkerCallout] are managed here.
 * This object is intended to work along with a [MapViewFragment].
 * After being created, the method [init] has to be called.
 *
 * @author P.Laurence on 09/04/17 -- Converted to Kotlin on 20/11/2020
 */
class MarkerLayer(
        private val mapLoader: MapLoader, private val scope: CoroutineScope
) : MarkerTapListener {
    var markers: List<Marker>? = null
    private var mapView: MapView? = null
    private var map: Map? = null

    override fun onMarkerTap(view: View, x: Int, y: Int) {
        val mapView = this.mapView ?: return
        val map = this.map ?: return
        if (view !is MovableMarker) return

        /* Prepare the callout */
        val markerCallout = MarkerCallout(mapView.context)
        markerCallout.setMoveAction(makeMorphMarkerRunnable(view, markerCallout,
                mapView, map, mapLoader))
        markerCallout.setEditAction(makeEditMarkerRunnable(map.id, view,
                markerCallout, mapView))
        markerCallout.setDeleteAction(makeDeleteMarkerRunnable(view, markerCallout,
                mapView, map, mapLoader))
        val marker = view.marker
        markerCallout.setTitle(marker.name)
        markerCallout.setSubTitle(marker.lat, marker.lon)
        val calloutHeight = 120.px
        val markerHeight = 48.px // The view height is 48dp, but only the top half is used to draw the marker.
        val calloutWidth = 200.px
        val markerWidth = 24.px
        val (relativeAnchorTop, absoluteAnchorTop, relativeAnchorLeft,
                absoluteAnchorLeft) = positionCallout(mapView, calloutWidth, calloutHeight,
                view.relativeX, view.relativeY, markerWidth, markerHeight)
        mapView.addCallout(markerCallout, view.relativeX, view.relativeY, relativeAnchorLeft,
                relativeAnchorTop, absoluteAnchorLeft, absoluteAnchorTop)
        markerCallout.transitionIn()

    }

    /**
     * Triggers the fetch of the map's markers and their drawing on the [MapView]. If this is
     * the first time this method is called for this map, the markers aren't defined and the
     * [MapLoader] will retrieve them. Otherwise, we can draw them immediately.
     *
     * This must be called when the [MapViewFragment] is ready to update its UI.
     */
    fun init(map: Map, mapView: MapView) {
        this.map = map
        this.mapView = mapView
        if (map.areMarkersDefined()) {
            updateMarkers()
        } else {
            scope.launch {
                val success = mapLoader.getMarkersForMap(map)
                if (success) {
                    updateMarkers()
                }
            }
        }
    }

    fun updateMarkers() {
        val mapView = this.mapView ?: return
        val map = this.map ?: return

        markers = map.markers
        if (markers == null) return
        map.markers?.forEach { marker ->
            val movableMarker = MovableMarker(mapView.context, true, marker)
            if (map.projection == null) {
                movableMarker.relativeX = marker.lon
                movableMarker.relativeY = marker.lat
            } else {
                /* Take proj values, and fallback to lat-lon if they are null */
                movableMarker.relativeX = if (marker.proj_x != null) marker.proj_x else marker.lon
                movableMarker.relativeY = if (marker.proj_y != null) marker.proj_y else marker.lat
            }
            movableMarker.initStatic()
            mapView.addMarker(movableMarker, movableMarker.relativeX, movableMarker.relativeY, -0.5f, -0.5f, 0f, 0f)
        }
    }

    /**
     * Add a [MovableMarker] to the center of the [MapView].
     */
    fun addNewMarker() {
        val mapView = this.mapView ?: return
        val map = this.map ?: return

        /* Calculate the relative coordinates of the center of the screen */
        val x = mapView.scrollX + mapView.width / 2 - mapView.offsetX
        val y = mapView.scrollY + mapView.height / 2 - mapView.offsetY
        val coordinateTranslater = mapView.coordinateTranslater ?: return
        val relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mapView.scale)
        val relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mapView.scale)
        val movableMarker: MovableMarker

        /* Create a new marker and add it to the map */
        val newMarker = Marker()
        if (map.projection == null) {
            newMarker.lat = relativeY
            newMarker.lon = relativeX
        } else {
            newMarker.proj_x = relativeX
            newMarker.proj_y = relativeY
            val wgs84Coords: DoubleArray? = map.projection?.undoProjection(relativeX, relativeY)
            if (wgs84Coords != null) {
                newMarker.lon = wgs84Coords[0]
                newMarker.lat = wgs84Coords[1]
            }
        }

        /* Create the corresponding view */
        movableMarker = MovableMarker(mapView.context, false, newMarker)
        movableMarker.relativeX = relativeX
        movableMarker.relativeY = relativeY
        movableMarker.initRounded()
        map.addMarker(newMarker)

        /* Easily move the marker */attachMarkerGrab(movableMarker, mapView, map, mapLoader)
        mapView.addMarker(movableMarker, relativeX, relativeY, -0.5f, -0.5f, 0f, 0f)
    }

    /**
     * Make the runnable to invoke when an external component requests a [MovableMarker] to be moved.
     */
    private fun makeMorphMarkerRunnable(
            movableMarker: MovableMarker, markerCallout: MarkerCallout, mapView: MapView,
            map: Map, mapLoader: MapLoader,
    ): () -> Unit = {
        movableMarker.morphToDynamicForm()
        /* Easily move the marker */
        attachMarkerGrab(movableMarker, mapView, map, mapLoader)

        /* Use a trick to bring the marker to the foreground */
        mapView.removeMarker(movableMarker)
        mapView.addMarker(movableMarker, movableMarker.relativeX, movableMarker.relativeY, -0.5f, -0.5f, 0f, 0f)

        /* Remove the callout */
        mapView.removeCallout(markerCallout)
    }

    /**
     * Make the callback to invoke when a single-tap is detected on a [MarkerGrab] (e.g when
     * the associated [MovableMarker] can be moved).
     *
     * It does the following :
     *  * Morph the [MovableMarker] into its static form
     *  * Animate out and remove the [MarkerGrab] which help the user to move the [MovableMarker]
     *  * Update the [MarkerGson.Marker] associated with the relative coordinates of the
     * [MovableMarker]. Depending on the [Map] using a projection or not, those
     * relative coordinates are wgs84 or projected values.
     */
    private fun makeClickCallback(
            movableMarker: MovableMarker, markerGrab: MarkerGrab,
            mapView: MapView, map: Map, mapLoader: MapLoader
    ) = ClickCallback {
        movableMarker.morphToStaticForm()

        /* After the morph, remove the MarkerGrab */
        markerGrab.morphOut(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                mapView.removeMarker(markerGrab)
                val l = markerGrab.onTouchMoveListener
                if (l != null) {
                    mapView.removeReferentialListener(l)
                }
            }
        })

        /* The view has been moved, update the associated model object */
        val marker = movableMarker.marker
        if (map.projection == null) {
            marker.lon = movableMarker.relativeX
            marker.lat = movableMarker.relativeY
        } else {
            marker.proj_x = movableMarker.relativeX
            marker.proj_y = movableMarker.relativeY
            val wgs84Coords: DoubleArray? = map.projection?.undoProjection(marker.proj_x, marker.proj_y)
            if (wgs84Coords != null) {
                marker.lon = wgs84Coords[0]
                marker.lat = wgs84Coords[1]
            }
        }

        /* Save the changes on the markers.json file */
        scope.launch {
            mapLoader.saveMarkers(map)
        }
    }

    /**
     * Make the runnable to invoke when an external component requests a [MovableMarker] to be edited.
     */
    private fun makeEditMarkerRunnable(
            mapId: Int, movableMarker: MovableMarker, markerCallout: MarkerCallout, mapView: MapView
    ): () -> Unit = {

        val action = MapViewFragmentDirections.actionMapViewFragmentToMarkerManageFragment(mapId, movableMarker.marker)
        Navigation.findNavController(mapView).navigate(action)

        /* Remove the callout */
        mapView.removeCallout(markerCallout)
    }

    /**
     * Make the runnable to invoke when an external component requests a [MovableMarker] to be deleted.
     */
    private fun makeDeleteMarkerRunnable(
            movableMarker: MovableMarker, markerCallout: MarkerCallout, mapView: MapView, map: Map,
            mapLoader: MapLoader
    ): () -> Unit = {
        /* Remove the callout */
        mapView.removeCallout(markerCallout)

        /* Delete the marker */
        mapView.removeMarker(movableMarker)
        val marker = movableMarker.marker
        scope.launch {
            mapLoader.deleteMarker(map, marker)
        }
    }

    /**
     * A [MarkerGrab] is used along with a [TouchMoveListener] to reflect its
     * displacement to the marker passed as argument.
     */
    private fun attachMarkerGrab(movableMarker: MovableMarker, mapView: MapView, map: Map,
                                 mapLoader: MapLoader) {
        /* Add a view as background, to move easily the marker */
        val markerMarkerMoveAgent = MarkerMoveAgent { _mapView: MapView, view: View, x: Double, y: Double ->
            _mapView.moveMarker(view, x, y)
            _mapView.moveMarker(movableMarker, x, y)
            movableMarker.relativeX = x
            movableMarker.relativeY = y
        }
        val markerGrab = MarkerGrab(mapView.context)

        val markerClickCallback = makeClickCallback(movableMarker, markerGrab, mapView, map, mapLoader)
        val touchMoveListener = TouchMoveListener(mapView, markerClickCallback, markerMarkerMoveAgent)
        mapView.addReferentialListener(touchMoveListener)
        markerGrab.onTouchMoveListener = touchMoveListener
        mapView.addMarker(markerGrab, movableMarker.relativeX, movableMarker.relativeY, -0.5f, -0.5f, 0f, 0f)
        markerGrab.morphIn(null)
    }
}
