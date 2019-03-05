package com.peterlaurence.trekme.ui.mapview

import android.content.Context
import android.view.View
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.gson.Landmark
import com.peterlaurence.trekme.core.map.maploader.MapLoader.getLandmarksForMap
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
        // TODO: implement
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
        val newLandmark = if (map.projection == null) {
            Landmark("", relativeY, relativeX, 0.0, 0.0, "")
        } else {
            val wgs84Coords: DoubleArray? = map.projection!!.undoProjection(relativeX, relativeY)
            Landmark("", wgs84Coords?.get(1) ?: 0.0, wgs84Coords?.get(0)
                    ?: 0.0, relativeX, relativeY, "")
        }

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
        val markerMoveCallback = TouchMoveListener.MoveCallback { tileView, view, x, y ->
            tileView.moveMarker(view, x, y)
            tileView.moveMarker(movableLandmark, x, y)
            movableLandmark.relativeX = x
            movableLandmark.relativeY = y
        }

        val markerGrab = MarkerGrab(context)
        // TODO: define a click callback, like in MarkerLayer
        markerGrab.setOnTouchListener(TouchMoveListener(tileView, markerMoveCallback))
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}