package com.peterlaurence.trekme.features.record.presentation.ui.components.elevationgraph

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.units.UnitFormatter.formatDistance
import com.peterlaurence.trekme.core.units.UnitFormatter.formatElevation
import com.peterlaurence.trekme.features.record.domain.model.ElePoint
import com.peterlaurence.trekme.util.dpToPx
import kotlinx.parcelize.Parcelize


class ElevationGraphView @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val axisPaint = Paint().apply {
        strokeWidth = dpToPx(2f)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = context.getColor(R.color.colorDarkGrey)
    }

    private val linePaint = Paint().apply {
        strokeWidth = dpToPx(2f)
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        color = context.getColor(R.color.colorAccent)
    }

    private val areaPaint = Paint().apply {
        strokeWidth = dpToPx(2f)
        style = Paint.Style.FILL
        isAntiAlias = true
        color = context.getColor(R.color.colorAccent)
        alpha = 100
    }

    private val elevationTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = dpToPx(12f)
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    private val elevationTextBgPaint = Paint().apply {
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
        strokeWidth = dpToPx(1f)
        style = Paint.Style.STROKE
        color = context.getColor(R.color.colorDarkGrey)
    }

    private val distancePaint = Paint().apply {
        textSize = dpToPx(12f)
        style = Paint.Style.FILL
        isAntiAlias = true
        color = context.getColor(R.color.colorDarkGrey)
    }

    private val paddingLeft = dpToPx(8f)
    private val paddingRight = paddingLeft
    private val paddingTop = dpToPx(8f)
    private val paddingBottom = dpToPx(16f)
    private val minElevationMargin = dpToPx(16f)
    private val maxElevationMargin = dpToPx(16f)
    private val maxDistanceMargin = dpToPx(16f)
    private val highlightEleTxtPadding = dpToPx(4f)
    private var highlightEleTxtOffsetX = 0f
    private val highlightEleTxtOffsetY = dpToPx(10f)
    private val pointRadius = dpToPx(4f)
    private var distTextOffsetX = 0f
    private var distTextOffsetY = 0f

    private var points: List<ElePoint>? = null
    private var distMax: Double? = null
    private var eleMax: Double? = null
    private var eleMin: Double? = null
    private var elevationProfile: Path? = null
    private var areaPath = Path()
    private var highlightPtX = 0f
    private var highlightPtY = 0f
    private val highlightPtRect = Rect()
    private val highlightPtBubble = RectF()
    private var eleText: String = ""
    private var distText: String = ""
    private var highLightLastValue: Float? = 0.5f

    /**
     * Set the list of [ElePoint], which is *assumed* to be sorted by distance.
     * In order to avoid the traversal cost of the provided list, the caller is responsible for
     * providing the actual minimum and maximum elevations. Failure to provide correct values will
     * result in an incorrect render.
     */
    fun setPoints(points: List<ElePoint>, eleMin: Double, eleMax: Double) = post {
        val distMax = points.lastOrNull()?.dist ?: return@post
        this.distMax = distMax
        this.points = points
        this.eleMax = eleMax
        this.eleMin = eleMin

        val elevationProfile = points.toPath(distMax, eleMax, eleMin)
        this.elevationProfile = elevationProfile
        if (elevationProfile != null) {
            areaPath = makeArea(elevationProfile, distMax)
        }
        highLightLastValue?.also {
            setHighlightPt(it)
        }
        invalidate()
    }

    /**
     * Highlight and show the elevation of a point of the elevation profile, given the proportion
     * of the total distance.
     *
     * @param progress As [Float] between 0f and 1f
     */
    fun setHighlightPt(progress: Float) = post {
        highLightLastValue = progress
        val points = points ?: return@post
        val distMax = distMax ?: return@post
        if (points.isEmpty()) return@post
        val eleMin = eleMin
        val eleMax = eleMax
        val eleRange = if (eleMin != null && eleMax != null) eleMax - eleMin else return@post

        val virtualPoint = if (points.size == 1) {
            points[0]
        } else {
            val virtDist = progress * distMax
            val nextDistIndex = points.indexOfFirst { it.dist > virtDist }.let {
                if (it == -1) points.lastIndex else it
            }
            val prevPt = points[(nextDistIndex - 1).coerceAtLeast(0)]
            val nextPt = points[nextDistIndex]
            val factor = (virtDist - prevPt.dist) / (nextPt.dist - prevPt.dist)
            val ele = factor * nextPt.elevation + (1 - factor) * prevPt.elevation
            ElePoint(virtDist, ele)
        }

        highlightPtX = translateX(virtualPoint.dist, distMax)
        highlightPtY = translateY(virtualPoint.elevation - eleMin, eleRange, distMax)

        eleText = formatElevation(virtualPoint.elevation)
        distText = formatDistance(virtualPoint.dist)

        computeEleTextBubble(eleText)
        computeDistTextOffset(distText)
        invalidate()
    }

    /**
     * Returns the numbers of pixels eaten in padding. Client code uses this information to know
     * the width in pixels of the usable part of the graph.
     */
    fun getDrawingPadding(): Int {
        return paddingLeft.toInt() + paddingRight.toInt() + maxDistanceMargin.toInt()
    }

    private fun makeArea(elevationLine: Path, distMax: Double): Path {
        val path = Path()
        path.addPath(elevationLine)
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

    private fun List<ElePoint>.toPath(distMax: Double, eleMax: Double, eleMin: Double): Path? {
        val path = Path()
        val firstPt: ElePoint = firstOrNull() ?: return null
        val x0 = translateX(0.0, distMax)
        val y0 = translateY(firstPt.elevation - eleMin, eleMax - eleMin, distMax)
        path.moveTo(x0, y0)
        for (point in this) {
            path.lineTo(
                    translateX(point.dist, distMax),
                    translateY(point.elevation - eleMin, eleMax - eleMin, distMax)
            )
        }

        return path
    }

    private fun translateX(x: Double, xRange: Double): Float {
        return paddingLeft + ((width - paddingLeft - paddingRight - maxDistanceMargin) * x / xRange).toFloat()
    }

    private fun translateY(y: Double, yRange: Double, distMax: Double): Float {
        val ratio = (yRange / distMax).coerceIn(0.20..1.0)
        return height - paddingBottom - minElevationMargin -
                ((height - paddingBottom - paddingTop - minElevationMargin - maxElevationMargin) * y * ratio / yRange).toFloat()
    }

    init {
        setWillNotDraw(false)
    }


    override fun onDraw(canvas: Canvas) {
        val elevationLine = elevationProfile ?: return
        val xOrig = paddingLeft
        val yOrig = height - paddingBottom

        /* Elevation line */
        canvas.drawPath(elevationLine, linePaint)

        /* Area */
        canvas.drawPath(areaPath, areaPaint)

        /* Axis */
        canvas.drawLine(xOrig, yOrig, width - paddingRight, yOrig, axisPaint)
        canvas.drawLine(xOrig, yOrig, xOrig, paddingTop, axisPaint)

        /* Elevation text and bubble */
        canvas.drawRoundRect(
                highlightPtBubble, 6f, 6f, elevationTextBgPaint
        )
        canvas.drawText(eleText,
                highlightPtX - highlightEleTxtOffsetX,
                highlightPtY - highlightEleTxtOffsetY, elevationTextPaint)

        /* Grey line */
        canvas.drawLine(highlightPtX, highlightPtY, highlightPtX, yOrig, greyLinePaint)

        /* Point */
        canvas.drawCircle(highlightPtX, highlightPtY, pointRadius, pointPaint)

        /* Distance text */
        canvas.drawText(distText, highlightPtX + distTextOffsetX, yOrig + distTextOffsetY, distancePaint)

        super.onDraw(canvas)
    }

    private fun computeEleTextBubble(eleText: String) {
        elevationTextPaint.getTextBounds(eleText, 0, eleText.length, highlightPtRect)
        val b = highlightPtRect
        val p = highlightEleTxtPadding
        val offsetX = if (highlightPtX - (b.right - b.left) / 2f - p < paddingLeft) {
            highlightPtX - paddingLeft - p
        } else if (highlightPtX + (b.right - b.left) / 2f + p > right) {
            highlightPtX + b.right + p - right + paddingLeft
        } else {
            (b.right - b.left) / 2f
        }
        highlightEleTxtOffsetX = offsetX
        val offsetY = highlightEleTxtOffsetY

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
        val p = highlightEleTxtPadding
        distTextOffsetX = if (highlightPtX + (r.right - r.left) / 2 > right - paddingLeft) {
            right - highlightPtX - paddingLeft + r.left - r.right - p
        } else if (highlightPtX - (r.right - r.left) / 2 < paddingLeft) {
            paddingLeft - highlightPtX
        } else {
            (r.left - r.right) / 2f
        }

        distTextOffsetY = r.bottom - r.top + dpToPx(2f)
    }

    override fun onSaveInstanceState(): Parcelable {
        val parentState = super.onSaveInstanceState() ?: Bundle()
        return SavedState(parentState, highLightLastValue)
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        val st = state as? SavedState
        if (st != null) {
            highLightLastValue = st.highLightVal
        }
    }
}

@Parcelize
private data class SavedState(val parcelable: Parcelable, val highLightVal: Float?) : View.BaseSavedState(parcelable)