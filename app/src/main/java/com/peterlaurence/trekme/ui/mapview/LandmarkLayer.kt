package com.peterlaurence.trekme.ui.mapview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.view.View
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.Landmark
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.ui.mapview.components.LandmarkCallout
import com.peterlaurence.trekme.ui.mapview.components.LineView
import com.peterlaurence.trekme.ui.mapview.components.MarkerGrab
import com.peterlaurence.trekme.ui.mapview.components.MovableLandmark
import com.peterlaurence.trekme.ui.mapview.controller.positionCallout
import com.peterlaurence.trekme.ui.tools.TouchMoveListener
import com.peterlaurence.trekme.util.px
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ovh.plrapps.mapview.MapView
import ovh.plrapps.mapview.ReferentialData
import ovh.plrapps.mapview.ReferentialListener
import ovh.plrapps.mapview.api.*
import ovh.plrapps.mapview.markers.MarkerTapListener

class LandmarkLayer(
        val context: Context,
        private val scope: CoroutineScope,
        private val mapLoader: MapLoader
) : MarkerTapListener, ReferentialListener, CoroutineScope by scope {
    private var map: Map? = null
    private var mapView: MapView? = null
    private var lastKnownPosition: Pair<Double, Double> = Pair(0.0, 0.0)
    private val movableLandmarkList: MutableList<MovableLandmark> = mutableListOf()
    private var touchMoveListener: TouchMoveListener? = null

    var referentialData = ReferentialData(false, 0f, 1f, 0.0, 0.0)
        set(value) {
            field = value

            movableLandmarkList.forEach {
                it.getLineView()?.onReferentialChanged(value)
                touchMoveListener?.onReferentialChanged(value)
            }
        }

    override fun onReferentialChanged(refData: ReferentialData) {
        referentialData = refData
    }

    fun init(map: Map, mapView: MapView) {
        this.map = map
        setMapView(mapView)
        mapView.addReferentialListener(this)

        if (map.areLandmarksDefined()) {
            drawLandmarks()
        } else {
            acquireThenDrawLandmarks()
        }
    }

    fun destroy() {
        mapView?.removeReferentialListener(this)
    }

    private fun CoroutineScope.acquireThenDrawLandmarks() = launch {
        val map = map ?: return@launch
        mapLoader.getLandmarksForMap(map)
        drawLandmarks()
    }

    private fun drawLandmarks() {
        val map = map ?: return
        val landmarks = map.landmarkGson.landmarks

        for (landmark in landmarks) {
            val movableLandmark = MovableLandmark(context, true, landmark, newLineView())
            if (map.projection == null) {
                movableLandmark.relativeX = landmark.lon
                movableLandmark.relativeY = landmark.lat
            } else {
                /* Take proj values, and fallback to lat-lon if they are null */
                movableLandmark.relativeX = if (landmark.proj_x != null) landmark.proj_x else landmark.lon
                movableLandmark.relativeY = if (landmark.proj_y != null) landmark.proj_y else landmark.lat
            }
            movableLandmark.initStatic()

            /* Keep a reference on it */
            movableLandmarkList.add(movableLandmark)

            mapView?.addMarker(movableLandmark, movableLandmark.relativeX!!,
                    movableLandmark.relativeY!!, -0.5f, -0.5f)
        }
    }

    fun addNewLandmark() {
        val mapView = this.mapView ?: return
        val map = map ?: return

        /* Calculate the relative coordinates of the center of the screen */
        val x = mapView.scrollX + mapView.width / 2 - mapView.offsetX
        val y = mapView.scrollY + mapView.height / 2 - mapView.offsetY
        val coordinateTranslater = mapView.coordinateTranslater ?: return
        val relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mapView.scale)
        val relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mapView.scale)

        val movableLandmark: MovableLandmark

        /* Create a new landmark and add it to the map */
        val newLandmark = Landmark("", 0.0, 0.0, 0.0, 0.0, "").newCoords(relativeX, relativeY, map)

        /* Create the corresponding view */
        movableLandmark = MovableLandmark(context, false, newLandmark, newLineView())
        movableLandmark.relativeX = relativeX
        movableLandmark.relativeY = relativeY
        movableLandmark.initRounded()

        /* Keep a reference on it */
        movableLandmarkList.add(movableLandmark)

        map.addLandmark(newLandmark)

        /* Easily move the marker */
        attachMarkerGrab(movableLandmark, map, context)

        mapView.addMarker(movableLandmark, relativeX, relativeY, -0.5f, -0.5f)
    }

    private fun attachMarkerGrab(movableLandmark: MovableLandmark, map: Map, context: Context) {
        /* Add a view as background, to move easily the marker */
        val landmarkMoveAgent = TouchMoveListener.MarkerMoveAgent { mapView, view, x, y ->
            mapView.moveMarker(view, x, y)
            mapView.moveMarker(movableLandmark, x, y)
            movableLandmark.relativeX = x
            movableLandmark.relativeY = y
            movableLandmark.updateLine()
        }

        val markerGrab = MarkerGrab(context)

        val landmarkClickCallback = TouchMoveListener.ClickCallback {
            movableLandmark.morphToStaticForm()

            /* After the morph, remove the MarkerGrab */
            markerGrab.morphOut(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    this@LandmarkLayer.mapView?.removeMarker(markerGrab)
                }
            })

            /* The view has been moved, update the associated model object */
            val landmark = movableLandmark.getLandmark()
            if (movableLandmark.relativeX != null && movableLandmark.relativeY != null) {
                landmark?.newCoords(movableLandmark.relativeX!!, movableLandmark.relativeY!!, map)
            }

            /* Save the changes on the markers.json file */
            scope.launch {
                mapLoader.saveLandmarks(map)
            }
        }

        touchMoveListener = TouchMoveListener(mapView ?: return, landmarkClickCallback, landmarkMoveAgent)
        touchMoveListener?.onReferentialChanged(referentialData)
        markerGrab.setOnTouchListener(touchMoveListener)
        if (movableLandmark.relativeX != null && movableLandmark.relativeY != null) {
            this.mapView?.addMarker(markerGrab, movableLandmark.relativeX!!, movableLandmark.relativeY!!, -0.5f, -0.5f)
            markerGrab.morphIn()
        }
    }

    private fun setMapView(mapView: MapView) {
        this.mapView = mapView
    }

    override fun onMarkerTap(view: View, x: Int, y: Int) {
        val mapView = mapView ?: return
        val map = map ?: return

        if (view is MovableLandmark && view.relativeX != null && view.relativeY != null) {
            val relativeX = view.relativeX ?: return
            val relativeY = view.relativeY ?: return

            /* Prepare the callout */
            val landmarkCallout = LandmarkCallout(context)
            landmarkCallout.setMoveAction {
                view.morphToDynamicForm()

                /* Easily move the landmark */
                attachMarkerGrab(view, map, context)

                /* Use a trick to bring the landmark to the foreground */
                mapView.removeMarker(view)
                mapView.addMarker(view, relativeX, relativeY, -0.5f, -0.5f)

                /* Remove the callout */
                mapView.removeCallout(landmarkCallout)
            }

            landmarkCallout.setDeleteAction {
                /* Remove the callout */
                mapView.removeCallout(landmarkCallout)

                /* Delete the landmark */
                mapView.removeMarker(view)
                movableLandmarkList.remove(view)
                view.deleteLine()

                val landmark = view.getLandmark()
                if (landmark != null) {
                    scope.launch {
                        mapLoader.deleteLandmark(map, landmark)
                    }
                }
            }
            view.getLandmark()?.also {
                landmarkCallout.setSubTitle(it.lat, it.lon)
            }

            val calloutHeight = 140.px
            val markerHeight = 48.px // The view height is 48dp, but only the top half is used to draw the marker.
            val calloutWidth = 100.px
            val markerWidth = 24.px

            val pos = positionCallout(mapView, calloutWidth, calloutHeight, relativeX, relativeY, markerWidth, markerHeight)

            mapView.addCallout(landmarkCallout, relativeX, relativeY, pos.relativeAnchorLeft,
                    pos.relativeAnchorTop, pos.absoluteAnchorLeft, pos.absoluteAnchorTop)

            landmarkCallout.transitionIn()
        }
    }

    private fun Landmark.newCoords(relativeX: Double, relativeY: Double, map: Map): Landmark = apply {
        if (map.projection == null) {
            lat = relativeY
            lon = relativeX
        } else {
            val wgs84Coords: DoubleArray? = map.projection!!.undoProjection(relativeX, relativeY)
            lat = wgs84Coords?.get(1) ?: 0.0
            lon = wgs84Coords?.get(0) ?: 0.0
            proj_x = relativeX
            proj_y = relativeY
        }
    }

    /**
     * Remove the associated [LineView]
     */
    private fun MovableLandmark.deleteLine() {
        val lineView = getLineView()
        mapView?.removeView(lineView)
    }

    private fun newLineView(): LineView {
        val lineView = LineView(context, referentialData, -0x3363d850)
        /* The index 1 is due to how MapView is designed and how we want landmarks to render (which
         * is above the map but beneath markers) */
        mapView?.addView(lineView, 1)
        return lineView
    }

    private fun MovableLandmark.updateLine() {
        val lineView = getLineView()
        if (relativeX != null && relativeY != null && lineView != null) {
            val coordinateTranslater = mapView?.coordinateTranslater ?: return

            lineView.updateLine(
                    coordinateTranslater.translateX(lastKnownPosition.first).toFloat(),
                    coordinateTranslater.translateY(lastKnownPosition.second).toFloat(),
                    coordinateTranslater.translateX(relativeX!!).toFloat(),
                    coordinateTranslater.translateY(relativeY!!).toFloat())
        }
    }

    /**
     * Called by the parent view ([MapViewFragment]).
     * All [LineView]s need to be updated.
     *
     * @param x the projected X coordinate, or longitude if there is no [Projection]
     * @param y the projected Y coordinate, or latitude if there is no [Projection]
     */
    fun onPositionUpdate(x: Double, y: Double) {
        lastKnownPosition = Pair(x, y)
        updateAllLines()
    }

    private fun updateAllLines() {
        movableLandmarkList.forEach {
            it.updateLine()
        }
    }
}