package com.peterlaurence.trekme.ui.mapview

import android.content.Context
import android.graphics.drawable.Animatable2
import android.graphics.drawable.Drawable
import android.view.View
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.Landmark
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.map.maploader.MapLoader.getLandmarksForMap
import com.peterlaurence.trekme.ui.mapview.components.LandmarkCallout
import com.peterlaurence.trekme.ui.mapview.components.MarkerGrab
import com.peterlaurence.trekme.ui.mapview.components.MovableLandmark
import com.peterlaurence.trekme.ui.tools.TouchMoveListener
import com.qozix.tileview.TileView
import com.qozix.tileview.markers.MarkerLayout
import kotlinx.coroutines.CoroutineScope

class LandmarkLayer(val context: Context, private val coroutineScope: CoroutineScope) :
        MarkerLayout.MarkerTapListener, CoroutineScope by coroutineScope {
    private lateinit var map: Map
    private lateinit var tileView: TileView
    private var visible = false

    fun init(map: Map, tileView: TileView) {
        this.map = map
        setTileView(tileView)

        if (map.areLandmarksDefined()) {
            drawLandmarks()
        } else {
            acquireThenDrawLandmarks()
        }
    }

    private fun acquireThenDrawLandmarks() {
        getLandmarksForMap(map).invokeOnCompletion {
            drawLandmarks()
        }
    }

    private fun drawLandmarks() {
        val landmarks = map.landmarkGson.landmarks

        for (landmark in landmarks) {
            val movableLandmark = MovableLandmark(context, true, landmark)
            if (map.projection == null) {
                movableLandmark.relativeX = landmark.lon
                movableLandmark.relativeY = landmark.lat
            } else {
                /* Take proj values, and fallback to lat-lon if they are null */
                movableLandmark.relativeX = if (landmark.proj_x != null) landmark.proj_x else landmark.lon
                movableLandmark.relativeY = if (landmark.proj_y != null) landmark.proj_y else landmark.lat
            }
            movableLandmark.initStatic()

            tileView.addMarker(movableLandmark, movableLandmark.relativeX!!,
                    movableLandmark.relativeY!!, -0.5f, -0.5f)
        }
    }

    fun addNewLandmark() {
        /* Calculate the relative coordinates of the center of the screen */
        val x = tileView.scrollX + tileView.width / 2 - tileView.offsetX
        val y = tileView.scrollY + tileView.height / 2 - tileView.offsetY
        val coordinateTranslater = tileView.coordinateTranslater
        val relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, tileView.scale)
        val relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, tileView.scale)

        val movableLandmark: MovableLandmark

        /* Create a new landmark and add it to the map */
        val newLandmark = Landmark("", 0.0, 0.0, 0.0, 0.0, "").newCoords(relativeX, relativeY)

        /* Create the corresponding view */
        movableLandmark = MovableLandmark(context, false, newLandmark)
        movableLandmark.relativeX = relativeX
        movableLandmark.relativeY = relativeY
        movableLandmark.initRounded()

        map.addLandmark(newLandmark)

        /* Easily move the marker */
        attachMarkerGrab(movableLandmark, tileView, map, context)

        tileView.addMarker(movableLandmark, relativeX, relativeY, -0.5f, -0.5f)
    }

    private fun attachMarkerGrab(movableLandmark: MovableLandmark, tileView: TileView, map: Map, context: Context) {
        /* Add a view as background, to move easily the marker */
        val landmarkMoveCallback = TouchMoveListener.MoveCallback { tileView, view, x, y ->
            tileView.moveMarker(view, x, y)
            tileView.moveMarker(movableLandmark, x, y)
            movableLandmark.relativeX = x
            movableLandmark.relativeY = y
        }

        val markerGrab = MarkerGrab(context)

        val landmarkClickCallback = TouchMoveListener.ClickCallback {
            movableLandmark.morphToStaticForm()

            /* After the morph, remove the MarkerGrab */
            markerGrab.morphOut(object : Animatable2.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable) {
                    super.onAnimationEnd(drawable)
                    tileView.removeMarker(markerGrab)
                }
            })

            /* The view has been moved, update the associated model object */
            val landmark = movableLandmark.getLandmark()
            if (movableLandmark.relativeX != null && movableLandmark.relativeY != null) {
                landmark.newCoords(movableLandmark.relativeX!!, movableLandmark.relativeY!!)
            }

            /* Save the changes on the markers.json file */
            MapLoader.saveLandmarks(map)
        }

        markerGrab.setOnTouchListener(TouchMoveListener(tileView, landmarkMoveCallback, landmarkClickCallback))
        if (movableLandmark.relativeX != null && movableLandmark.relativeY != null) {
            tileView.addMarker(markerGrab, movableLandmark.relativeX!!, movableLandmark.relativeY!!, -0.5f, -0.5f)
            markerGrab.morphIn()
        }
    }

    /**
     * Return a copy of the private [visible] flag.
     */
    fun isVisible() = visible

    private fun setTileView(tileView: TileView) {
        this.tileView = tileView
    }

    override fun onMarkerTap(view: View?, x: Int, y: Int) {
        if (view is MovableLandmark && view.relativeX != null && view.relativeY != null) {
            /* Prepare the callout */
            val landmarkCallout = LandmarkCallout(context)
            landmarkCallout.setMoveAction {
                view.morphToDynamicForm()

                /* Easily move the landmark */
                attachMarkerGrab(view, tileView, map, context)

                /* Use a trick to bring the landmark to the foreground */
                tileView.removeMarker(view)
                tileView.addMarker(view, view.relativeX!!, view.relativeY!!, -0.5f, -0.5f)

                /* Remove the callout */
                tileView.removeCallout(landmarkCallout)
            }

            landmarkCallout.setDeleteAction {
                /* Remove the callout */
                tileView.removeCallout(landmarkCallout)

                /* Delete the marker */
                tileView.removeMarker(view)

                val landmark = view.getLandmark()
                MapLoader.deleteLandmark(map, landmark)
            }
            val landmark = view.getLandmark()
            landmarkCallout.setSubTitle(landmark.lat, landmark.lon)

            tileView.addCallout(landmarkCallout, view.relativeX!!, view.relativeY!!, -0.5f, -1.2f)

            landmarkCallout.transitionIn()
        }
    }

    private fun Landmark.newCoords(relativeX: Double, relativeY: Double): Landmark = apply {
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
}