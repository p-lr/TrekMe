package com.peterlaurence.trekme.ui.record.components.widgets

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.util.formatDistance
import com.peterlaurence.trekme.util.px
import com.peterlaurence.trekme.viewmodel.record.AltPoint


class AltitudeGraphView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val axisPaint = Paint().apply {
        strokeWidth = 2.px.toFloat()
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
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

    private val altitudeTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 12.px.toFloat()
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val altitudeTextBgPaint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.FILL
        alpha = 180
    }

    private val pointPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
        color = context.getColor(R.color.colorAccent)
    }

    private val greyLinePaint = Paint().apply {
        strokeWidth = 1.px.toFloat()
        style = Paint.Style.STROKE
        color = context.getColor(R.color.colorDarkGrey)
    }

    private val distancePaint = Paint().apply {
        textSize = 12.px.toFloat()
        style = Paint.Style.FILL
        isAntiAlias = true
        color = context.getColor(R.color.colorDarkGrey)
    }

    private val paddingLeft = 8.px.toFloat()
    private val paddingRight = paddingLeft
    private val paddingTop = 8.px.toFloat()
    private val paddingBottom = 16.px.toFloat()
    private val minAltitudeMargin = 16.px.toFloat()
    private val maxAltitudeMargin = 16.px.toFloat()
    private val maxDistanceMargin = 16.px.toFloat()
    private val highlightAltTxtPadding = 4.px.toFloat()
    private var highlightAltTxtOffsetX = 0f
    private val highlightAltTxtOffsetY = 10.px.toFloat()
    private val pointRadius = 4.px.toFloat()
    private var distTextOffsetX = 0f
    private var distTextOffsetY = 0f

    private var points: List<AltPoint>? = null
    private var distMax: Double? = null
    private var altMax: Double? = null
    private var altMin: Double? = null
    private var altitudeProfile: Path? = null
    private var areaPath = Path()
    private var highlightPtX = 0f
    private var highlightPtY = 0f
    private val highlightPtRect = Rect()
    private val highlightPtBubble = RectF()
    private var altText: String = ""
    private var distText: String = ""

    fun setPoints(points: List<AltPoint>, altMin: Double, altMax: Double) = post {
        val distMax = points.lastOrNull()?.dist ?: return@post
        this.distMax = distMax
        this.points = points
        this.altMax = altMax
        this.altMin = altMin

        altitudeProfile = points.toPath(distMax, altMax, altMin)
        areaPath = makeArea(altitudeProfile!!, distMax, altMax)
        invalidate()
    }

    /**
     * Highlight and show the altitude of a point of the altitude profile, given the percent of the
     * total distance.
     */
    fun setHighlightPt(percent: Int) = post {
        val points = points ?: return@post
        val distMax = distMax ?: return@post
        val altMin = altMin
        val altMax = altMax
        val altRange = if (altMin != null && altMax != null) altMax - altMin else return@post

        /* A dummy impl */
        // TODO : finish this
        val index = 0
        highlightPtX = translateX(points[index].dist, distMax)
        highlightPtY = translateY(points[index].altitude, altRange)

        altText = formatDistance(points[index].altitude)
        distText = formatDistance(points[index].dist)
        computeAltTextBubble(altText)
        computeDistTextOffset(distText)
    }

    private fun makeArea(altitudeLine: Path, distMax: Double, altMax: Double): Path {
        val path = Path()
        path.addPath(altitudeLine)
        path.lineTo(
                translateX(distMax, distMax),
                height.toFloat() - paddingBottom
        )
        path.lineTo(
                translateX(0.0, distMax),
                height.toFloat() - paddingBottom
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
        return paddingLeft + ((width - paddingLeft - paddingRight - maxDistanceMargin) * x / xRange).toFloat()
    }

    private fun translateY(y: Double, yRange: Double): Float {
        return height - paddingBottom - minAltitudeMargin -
                ((height - paddingBottom - paddingTop - minAltitudeMargin - maxAltitudeMargin) * y / yRange).toFloat()
    }

    init {
        setWillNotDraw(false)
        setPoints(listOf(
                AltPoint(0.0, 55.0),
                AltPoint(150.0, 100.0),
                AltPoint(1000.0, 15.0),
                AltPoint(1500.0, 30.0)
        ), 0.0, 100.0)
        setHighlightPt(50)
    }


    override fun onDraw(canvas: Canvas) {
        val altitudeLine = altitudeProfile ?: return
        val xOrig = paddingLeft
        val yOrig = height - paddingBottom

        /* Altitude line */
        canvas.drawPath(altitudeLine, linePaint)

        /* Area */
        canvas.drawPath(areaPath, areaPaint)

        /* Axis */
        canvas.drawLine(xOrig, yOrig, width - paddingRight, yOrig, axisPaint)
        canvas.drawLine(xOrig, yOrig, xOrig, paddingTop, axisPaint)

        /* Altitude text and bubble */
        canvas.drawRoundRect(
                highlightPtBubble, 6f, 6f, altitudeTextBgPaint
        )
        canvas.drawText(altText,
                highlightPtX - highlightAltTxtOffsetX,
                highlightPtY - highlightAltTxtOffsetY, altitudeTextPaint)

        /* Grey line */
        canvas.drawLine(highlightPtX, highlightPtY, highlightPtX, yOrig, greyLinePaint)

        /* Point */
        canvas.drawCircle(highlightPtX, highlightPtY, pointRadius, pointPaint)

        /* Distance text */
        canvas.drawText(distText, highlightPtX + distTextOffsetX, yOrig + distTextOffsetY, distancePaint)

        super.onDraw(canvas)
    }

    private fun computeAltTextBubble(altText: String) {
        altitudeTextPaint.getTextBounds(altText, 0, altText.length, highlightPtRect)
        val b = highlightPtRect
        val p = highlightAltTxtPadding
        val offsetX = if (highlightPtX - (b.right - b.left) / 2f - p < paddingLeft) {
            highlightPtX - paddingLeft - p
        } else if (highlightPtX + (b.right - b.left) / 2f + p > right) {
            highlightPtX + b.right + p - right + paddingLeft
        } else {
            (b.right - b.left) / 2f
        }
        highlightAltTxtOffsetX = offsetX
        val offsetY = highlightAltTxtOffsetY

        with(highlightPtBubble) {
            left = highlightPtX + b.left.toFloat() - p - offsetX
            top = highlightPtY + b.top.toFloat() - p - offsetY
            right = highlightPtX + b.right.toFloat() + p - offsetX
            bottom = highlightPtY + b.bottom.toFloat() + p - offsetY
        }
    }

    private fun computeDistTextOffset(distText: String) {
        val r = Rect()
        distancePaint.getTextBounds(distText, 0, distText.length, r)
        val p = highlightAltTxtPadding
        distTextOffsetX = if (highlightPtX + (r.right - r.left) / 2 > right - paddingLeft) {
            right - highlightPtX - paddingLeft + r.left - r.right - p
        } else if (highlightPtX - (r.right - r.left) / 2 < paddingLeft) {
            paddingLeft - highlightPtX
        } else {
            (r.left - r.right) / 2f
        }

        distTextOffsetY = r.bottom - r.top + 2.px.toFloat()
    }
}