package com.peterlaurence.trekme.ui.mapcreate.components

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.peterlaurence.mapview.MapView
import com.peterlaurence.mapview.api.addMarker
import com.peterlaurence.mapview.api.moveMarker
import com.peterlaurence.mapview.api.removeMarker
import com.peterlaurence.trekme.ui.tools.TouchMoveListener

class AreaLayer(val context: Context, val areaListener: AreaListener) {
    lateinit var mapView: MapView
    private lateinit var areaView: AreaView
    private lateinit var areaMarkerFirst: AreaMarker
    private lateinit var areaMarkerSecond: AreaMarker
    private var visible = false

    private var firstMarkerRelativeX = 0.0
    private var firstMarkerRelativeY = 0.0
    private var secondMarkerRelativeX = 0.0
    private var secondMarkerRelativeY = 0.0

    /**
     * Shows the two [AreaMarker] and the [AreaView].
     */
    fun attachTo(mapView: MapView) {
        this.mapView = mapView

        /* Create the area view between the two markers */
        areaView = AreaView(context)
        mapView.addReferentialOwner(areaView)
        mapView.addView(areaView)

        /* Setup the first marker */
        areaMarkerFirst = AreaMarker(context)
        val firstMarkerMoveAgent = TouchMoveListener.MarkerMoveAgent { tileView_, _, x, y ->
            firstMarkerRelativeX = x
            firstMarkerRelativeY = y
            tileView_.moveMarker(areaMarkerFirst, x, y)
            onMarkerMoved()
        }
        val firstMarkerTouchMoveListener = TouchMoveListener(mapView, firstMarkerMoveAgent)
        areaMarkerFirst.setOnTouchListener(firstMarkerTouchMoveListener)

        /* Setup the second marker */
        areaMarkerSecond = AreaMarker(context)
        val secondMarkerMoveAgent = TouchMoveListener.MarkerMoveAgent { tileView_, _, x, y ->
            secondMarkerRelativeX = x
            secondMarkerRelativeY = y
            tileView_.moveMarker(areaMarkerSecond, x, y)
            onMarkerMoved()
        }
        val secondMarkerTouchMoveListener = TouchMoveListener(mapView, secondMarkerMoveAgent)
        areaMarkerSecond.setOnTouchListener(secondMarkerTouchMoveListener)

        /* Set their positions */
        initAreaMarkers()
        onMarkerMoved()

        /* ..and add them to the TileView */
        mapView.addMarker(areaMarkerFirst, firstMarkerRelativeX, firstMarkerRelativeY,
                -0.5f, -0.5f)
        mapView.addMarker(areaMarkerSecond, secondMarkerRelativeX, secondMarkerRelativeY,
                -0.5f, -0.5f)
        visible = true
    }

    /**
     * Removes the two [AreaMarker] and the [AreaView] from the [MapView].
     */
    fun detach() {
        if (::mapView.isInitialized) {
            mapView.removeMarker(areaMarkerFirst)
            mapView.removeMarker(areaMarkerSecond)
            mapView.removeView(areaView)
            mapView.removeReferentialOwner(areaView)
        }

        visible = false

        areaListener.hideArea()
    }

    private fun onMarkerMoved() {
        /* Update the ui */
        val translater = mapView.coordinateTranslater
        areaView.updateArea(
                translater.translateX(firstMarkerRelativeX).toFloat(),
                translater.translateY(firstMarkerRelativeY).toFloat(),
                translater.translateX(secondMarkerRelativeX).toFloat(),
                translater.translateY(secondMarkerRelativeY).toFloat())

        /* Notify the listener */
        areaListener.areaChanged(Area(firstMarkerRelativeX, firstMarkerRelativeY, secondMarkerRelativeX,
                secondMarkerRelativeY))
    }

    private fun initAreaMarkers() {
        /* Calculate the relative coordinates of the first marker */
        var x = mapView.scrollX + (mapView.width * 0.66f).toInt() - mapView.offsetX
        var y = mapView.scrollY + (mapView.height * 0.33f).toInt() - mapView.offsetY
        val coordinateTranslater = mapView.coordinateTranslater
        var relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mapView.scale)
        var relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mapView.scale)

        firstMarkerRelativeX = Math.min(relativeX, coordinateTranslater.right)
        firstMarkerRelativeY = Math.min(relativeY, coordinateTranslater.top)

        /* Calculate the relative coordinates of the second marker */
        x = mapView.scrollX + (mapView.width * 0.33f).toInt() - mapView.offsetX
        y = mapView.scrollY + (mapView.height * 0.66f).toInt() - mapView.offsetY
        relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mapView.scale)
        relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mapView.scale)

        secondMarkerRelativeX = Math.max(relativeX, coordinateTranslater.left)
        secondMarkerRelativeY = Math.max(relativeY, coordinateTranslater.bottom)
    }
}

interface AreaListener {
    fun areaChanged(area: Area)
    fun hideArea()
}

data class Area(val relativeX1: Double, val relativeY1: Double, val relativeX2: Double, val relativeY2: Double) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readDouble(),
            parcel.readDouble())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(relativeX1)
        parcel.writeDouble(relativeY1)
        parcel.writeDouble(relativeX2)
        parcel.writeDouble(relativeY2)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Area> {
        override fun createFromParcel(parcel: Parcel): Area {
            return Area(parcel)
        }

        override fun newArray(size: Int): Array<Area?> {
            return arrayOfNulls(size)
        }
    }
}
