package com.peterlaurence.trekme.ui.record.components.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.util.px
import com.peterlaurence.trekme.viewmodel.record.AltPoint


class AltitudeGraphView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val axisPaint = Paint().apply {
        strokeWidth = 2.px.toFloat()
        style = Paint.Style.STROKE
        color = context.getColor(R.color.colorDarkGrey)
    }

    private val linePaint = Paint().apply {
        strokeWidth = 2.px.toFloat()
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        color = context.getColor(R.color.colorAccent)
    }

    private val areaPaint = Paint().apply {
        strokeWidth = 2.px.toFloat()
        style = Paint.Style.FILL
        isAntiAlias = true
        color = context.getColor(R.color.colorAccent)
        alpha = 100
    }

    private val padding = 8.px.toFloat()
    private val minAltitudeMargin = 16.px.toFloat()
    private val maxAltitudeMargin = 16.px.toFloat()
    private val maxDistanceMargin = 16.px.toFloat()

    private var altitudeLine: Path? = null
    private var areaPath = Path()

    fun setPoints(points: List<AltPoint>, altMin: Double, altMax: Double) = post {
        val distMax = points.lastOrNull()?.dist ?: return@post

        altitudeLine = points.toPath(distMax, altMax, altMin)
        areaPath = makeArea(altitudeLine!!, distMax, altMax)
        invalidate()
    }

    private fun makeArea(altitudeLine: Path, distMax: Double, altMax: Double): Path {
        val path = Path()
        path.addPath(altitudeLine)
        path.lineTo(
                translateX(distMax, distMax),
                height.toFloat() - padding
        )
        path.lineTo(
                translateX(0.0, distMax),
                height.toFloat() - padding
        )

        path.close()
        return path
    }

    private fun List<AltPoint>.toPath(distMax: Double, altMax: Double, altMin: Double): Path? {
        if (size < 2) return null
        val path = Path()
        val firstPt: AltPoint = firstOrNull() ?: return null
        val x0 = translateX(0.0, distMax)
        val y0 = translateY(firstPt.altitude - altMin, altMax - altMin)
        path.moveTo(x0, y0)
        for (point in this) {
            path.lineTo(
                    translateX(point.dist, distMax),
                    translateY(point.altitude - altMin, altMax - altMin)
            )
        }

        return path
    }

    private fun translateX(x: Double, xRange: Double): Float {
        return padding + ((width - 2 * padding - maxDistanceMargin) * x / xRange).toFloat()
    }

    private fun translateY(y: Double, yRange: Double): Float {
        return height - padding - minAltitudeMargin -
                ((height - 2 * padding - minAltitudeMargin - maxAltitudeMargin) * y / yRange).toFloat()
    }

    init {
        setWillNotDraw(false)
        setPoints(listOf(
                AltPoint(0.0, 55.0),
                AltPoint(150.0, 100.0),
                AltPoint(1000.0, 0.0),
                AltPoint(1500.0, 30.0)
        ), 0.0, 100.0)
    }


    override fun onDraw(canvas: Canvas) {
        val altitudeLine = altitudeLine ?: return
        val xOrig = padding
        val yOrig = height - padding

        /* Altitude line */
        canvas.drawPath(altitudeLine, linePaint)

        /* Area */
        canvas.drawPath(areaPath, areaPaint)

        /* Axis */
        canvas.drawLine(xOrig, yOrig, width - padding, yOrig, axisPaint)
        canvas.drawLine(xOrig, yOrig, xOrig, padding, axisPaint)

        super.onDraw(canvas)
    }
}