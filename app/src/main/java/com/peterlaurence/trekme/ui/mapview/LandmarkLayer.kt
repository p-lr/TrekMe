package com.peterlaurence.trekme.ui.mapview

import android.content.Context
import android.view.View
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.ui.mapview.components.MarkerCallout
import com.peterlaurence.trekme.ui.mapview.components.MovableMarker
import com.qozix.tileview.TileView
import com.qozix.tileview.markers.MarkerLayout

class LandmarkLayer(val context: Context) {
    private lateinit var map: Map
    private lateinit var tileView: TileView
    private var visible = false

    fun init(map: Map, tileView: TileView) {
        this.map = map
        setTileView(tileView)
    }

    fun show() {

    }

    fun hide()  {

    }

    fun toggleVisibility(): Boolean {
        visible = !visible
        return visible
    }

    /**
     * Return a copy of the private [visible] flag.
     */
    fun isVisible() = visible

    private fun setTileView(tileView: TileView) {
        this.tileView = tileView

        this.tileView.setMarkerTapListener { view: View, x: Int, y: Int ->
            if (view is MovableMarker) {

                /* Prepare the callout */
                val markerCallout = MarkerCallout(context)
//                markerCallout.setMoveAction(MorphMarkerRunnable(view, markerCallout,
//                        mTileView, mContext, mMap))
//                markerCallout.setEditAction(EditMarkerRunnable(view, this@MarkerLayer,
//                        markerCallout, mTileView, mRequestManageMarkerListener))
//                markerCallout.setDeleteAction(DeleteMarkerRunnable(view, markerCallout,
//                        tileView, mMap))
                val marker = view.marker
                markerCallout.setTitle(marker.name)
                markerCallout.setSubTitle(marker.lat, marker.lon)

                this.tileView.addCallout(markerCallout, view.relativeX, view.relativeY, -0.5f, -1.2f)
                markerCallout.transitionIn()
            }
        }
    }

}