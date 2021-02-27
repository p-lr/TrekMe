package com.peterlaurence.trekme.ui.tools

import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import com.peterlaurence.trekme.ui.tools.TouchMoveListener.ClickCallback
import com.peterlaurence.trekme.ui.tools.TouchMoveListener.MarkerMoveAgent
import ovh.plrapps.mapview.MapView
import ovh.plrapps.mapview.ReferentialData
import ovh.plrapps.mapview.ReferentialListener
import ovh.plrapps.mapview.api.getConstrainedX
import ovh.plrapps.mapview.api.getConstrainedY
import ovh.plrapps.mapview.core.CoordinateTranslater
import kotlin.math.cos
import kotlin.math.sin

/**
 * A touch listener that enables touch-moves of a view (also called marker) on a [MapView].
 * The logic of moving the marker is delegated to the provided [MarkerMoveAgent].
 * It can also react to single-tap event. To be notified, provide a [ClickCallback] to
 * the overloaded constructor.
 *
 * Example of usage :
 * ```
 * MarkerMoveAgent agent = new ClassImplementsMoveAgent();
 * TouchMoveListener markerTouchListener = new TouchMoveListener(mapView, agent);
 * View marker = new CustomMarker(context);
 * marker.setOnTouchListener(markerTouchListener);
 * mapView.addMarker(marker, ...);
 * ```
 *
 * @author P.Laurence
 */
class TouchMoveListener @JvmOverloads constructor(private val mapView: MapView,
                                                  private val markerClickCallback: ClickCallback? = null,
                                                  private val markerMarkerMoveAgent: MarkerMoveAgent) : SimpleOnGestureListener(), OnTouchListener, ReferentialListener {
    private val mGestureDetector = GestureDetector(mapView.context, this)
    private var deltaX = 0f
    private var deltaY = 0f
    private var referentialData: ReferentialData? = null

    override fun onReferentialChanged(refData: ReferentialData) {
        this.referentialData = refData
    }

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (mGestureDetector.onTouchEvent(event)) {
            return true
        }
        val rd = referentialData
        var angle = 0.0
        if (rd != null) {
            angle = -Math.toRadians(rd.angle.toDouble())
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                deltaX = event.x
                deltaY = event.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dX: Float
                val dY: Float
                if (rd != null && rd.rotationEnabled) {
                    dX = ((event.x - deltaX) * cos(angle) - (event.y - deltaY) * sin(angle)).toFloat()
                    dY = ((event.x - deltaX) * sin(angle) + (event.y - deltaY) * cos(angle)).toFloat()
                } else {
                    dX = event.x - deltaX
                    dY = event.y - deltaY
                }
                val X: Double
                val Y: Double
                val ct = mapView.coordinateTranslater ?: return true
                if (rd != null && rd.rotationEnabled) {
                    val Xorig = ct.reverseRotationX(rd, view.x + (view.width shr 1), view.y + (view.height shr 1))
                    val Yorig = ct.reverseRotationY(rd, view.x + (view.width shr 1), view.y + (view.height shr 1))
                    X = getRelativeX(ct, Xorig.toFloat() + dX)
                    Y = getRelativeY(ct, Yorig.toFloat() + dY)
                } else {
                    X = getRelativeX(ct, view.x + dX + (view.width shr 1))
                    Y = getRelativeY(ct, view.y + dY + (view.height shr 1))
                }

                markerMarkerMoveAgent.onMarkerMove(mapView, view, getConstrainedX(X)
                        ?: return true, getConstrainedY(Y) ?: return true)
            }
            else -> return false
        }
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        markerClickCallback?.onMarkerClick()
        return true
    }

    private fun getRelativeX(ct: CoordinateTranslater, x: Float): Double {
        return ct.translateAndScaleAbsoluteToRelativeX(x.toInt(), mapView.scale)
    }

    private fun getRelativeY(ct: CoordinateTranslater, y: Float): Double {
        return ct.translateAndScaleAbsoluteToRelativeY(y.toInt(), mapView.scale)
    }

    private fun getConstrainedX(x: Double): Double? {
        return mapView.getConstrainedX(x)
    }

    private fun getConstrainedY(y: Double): Double? {
        return mapView.getConstrainedY(y)
    }

    /**
     * A [MarkerMoveAgent] is given the "relative coordinates" of the view added to the [MapView].
     * Most of the time, the callee sets the given coordinates of the view on the [MapView].
     */
    fun interface MarkerMoveAgent {
        fun onMarkerMove(mapView: MapView, view: View, x: Double, y: Double)
    }

    fun interface ClickCallback {
        fun onMarkerClick()
    }

}