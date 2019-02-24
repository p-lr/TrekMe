package com.peterlaurence.trekme.ui.mapview

import android.content.Context
import android.view.View
import com.peterlaurence.trekme.core.map.Map
import com.qozix.tileview.TileView
import com.qozix.tileview.markers.MarkerLayout

class LandmarkLayer(val context: Context) : MarkerLayout.MarkerTapListener {
    private lateinit var map: Map
    private lateinit var tileView: TileView
    private var visible = false

    fun init(map: Map, tileView: TileView) {
        this.map = map
        setTileView(tileView)
    }

    fun show() {

    }

    fun hide() {

    }

    fun addNewLandmark() {

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