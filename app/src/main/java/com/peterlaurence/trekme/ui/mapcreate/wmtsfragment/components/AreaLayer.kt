package com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.components

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import ovh.plrapps.mapview.MapView
import ovh.plrapps.mapview.api.*
import com.peterlaurence.trekme.ui.tools.TouchMoveListener
import kotlin.math.max
import kotlin.math.min

/**
 * Area selection logic. This layer is made of four views:
 * * An [AreaView],
 * * Two [AreaMarker]s,
 * * An [AreaMarkerCentral]
 */
class AreaLayer(val context: Context, private val areaListener: AreaListener) {
    lateinit var mapView: MapView
    private lateinit var areaView: AreaView
    private lateinit var areaMarkerFirst: AreaMarker
    private lateinit var areaMarkerSecond: AreaMarker
    private lateinit var areaMarkerCentral: AreaMarkerCentral
    private var visible = false

    private val centralMarkerRelativeX
        get() = (firstMarkerRelativeX + secondMarkerRelativeX) / 2
    private val centralMarkerRelativeY
        get() = (firstMarkerRelativeY + secondMarkerRelativeY) / 2

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
        mapView.addReferentialListener(areaView)
        mapView.addView(areaView)

        /* Setup the first marker */
        areaMarkerFirst = AreaMarker(context)
        val firstMarkerMoveAgent = TouchMoveListener.MarkerMoveAgent { mapView_, _, x, y ->
            firstMarkerRelativeX = x
            firstMarkerRelativeY = y
            mapView_.moveMarker(areaMarkerFirst, x, y)
            onMarkerMoved()
        }
        val firstMarkerTouchMoveListener = TouchMoveListener(mapView, null, firstMarkerMoveAgent)
        areaMarkerFirst.setOnTouchListener(firstMarkerTouchMoveListener)

        /* Setup the second marker */
        areaMarkerSecond = AreaMarker(context)
        val secondMarkerMoveAgent = TouchMoveListener.MarkerMoveAgent { mapView_, _, x, y ->
            secondMarkerRelativeX = x
            secondMarkerRelativeY = y
            mapView_.moveMarker(areaMarkerSecond, x, y)
            onMarkerMoved()
        }
        val secondMarkerTouchMoveListener = TouchMoveListener(mapView, null, secondMarkerMoveAgent)
        areaMarkerSecond.setOnTouchListener(secondMarkerTouchMoveListener)

        /* Setup the central marker */
        areaMarkerCentral = AreaMarkerCentral(context)
        val centralMarkerMoveAgent = TouchMoveListener.MarkerMoveAgent { mapView_, _, x, y ->
            onCentralMarkerMove(mapView_,
                    x - centralMarkerRelativeX,
                    y - centralMarkerRelativeY)
            onMarkerMoved()
        }
        val centralMarkerTouchMoveListener = TouchMoveListener(mapView, null, centralMarkerMoveAgent)
        areaMarkerCentral.setOnTouchListener(centralMarkerTouchMoveListener)

        /* Set their positions */
        initAreaMarkers()
        onMarkerMoved()

        /* ..and add them to the TileView */
        mapView.addMarker(areaMarkerCentral, centralMarkerRelativeX, centralMarkerRelativeY,
                -0.5f, -0.5f)
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
            mapView.removeMarker(areaMarkerCentral)
            mapView.removeView(areaView)
            mapView.removeReferentialListener(areaView)
        }

        visible = false

        areaListener.hideArea()
    }

    /**
     * Move the two markers in the corners.
     */
    private fun onCentralMarkerMove(mapView: MapView, deltaX: Double, deltaY: Double) {
        firstMarkerRelativeX += deltaX
        secondMarkerRelativeX += deltaX

        firstMarkerRelativeY += deltaY
        secondMarkerRelativeY += deltaY

        firstMarkerRelativeX = mapView.getConstrainedX(firstMarkerRelativeX) ?: return
        firstMarkerRelativeY = mapView.getConstrainedY(firstMarkerRelativeY) ?: return
        mapView.moveMarker(areaMarkerFirst, firstMarkerRelativeX, firstMarkerRelativeY)

        secondMarkerRelativeX = mapView.getConstrainedX(secondMarkerRelativeX) ?: return
        secondMarkerRelativeY = mapView.getConstrainedY(secondMarkerRelativeY) ?: return
        mapView.moveMarker(areaMarkerSecond, secondMarkerRelativeX, secondMarkerRelativeY)
    }

    private fun onMarkerMoved() {
        /* Update the ui */
        val translater = mapView.coordinateTranslater ?: return
        areaView.updateArea(
                translater.translateX(firstMarkerRelativeX).toFloat(),
                translater.translateY(firstMarkerRelativeY).toFloat(),
                translater.translateX(secondMarkerRelativeX).toFloat(),
                translater.translateY(secondMarkerRelativeY).toFloat())

        mapView.moveMarker(areaMarkerCentral, centralMarkerRelativeX, centralMarkerRelativeY)

        /* Notify the listener */
        areaListener.areaChanged(Area(firstMarkerRelativeX, firstMarkerRelativeY, secondMarkerRelativeX,
                secondMarkerRelativeY))
    }

    private fun initAreaMarkers() {
        /* Calculate the relative coordinates of the first marker */
        var x = mapView.scrollX + (mapView.width * 0.66f).toInt() - mapView.offsetX
        var y = mapView.scrollY + (mapView.height * 0.33f).toInt() - mapView.offsetY
        val coordinateTranslater = mapView.coordinateTranslater ?: return
        var relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mapView.scale)
        var relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mapView.scale)

        firstMarkerRelativeX = min(relativeX, coordinateTranslater.right)
        firstMarkerRelativeY = min(relativeY, coordinateTranslater.top)

        /* Calculate the relative coordinates of the second marker */
        x = mapView.scrollX + (mapView.width * 0.33f).toInt() - mapView.offsetX
        y = mapView.scrollY + (mapView.height * 0.66f).toInt() - mapView.offsetY
        relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mapView.scale)
        relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mapView.scale)

        secondMarkerRelativeX = max(relativeX, coordinateTranslater.left)
        secondMarkerRelativeY = max(relativeY, coordinateTranslater.bottom)
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
