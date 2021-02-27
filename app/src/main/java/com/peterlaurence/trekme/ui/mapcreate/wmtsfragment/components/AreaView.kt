package com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.TypedValue
import android.view.View
import com.peterlaurence.trekme.R
import ovh.plrapps.mapview.ReferentialData
import ovh.plrapps.mapview.ReferentialListener

/**
 * A custom view that draws a square between two [AreaMarker] and represents an area.
 *
 * @author P.Laurence on 12/05/18
 */
class AreaView(context: Context) : View(context), ReferentialListener {
    private val strokeWidth: Float
    private val paintBackground: Paint = Paint()
    private val paintStroke: Paint = Paint()
    private var mBackgroundColor: Int = Color.BLUE
    private var mStrokeColor: Int = Color.BLUE
    private var x1: Float = 0f
    private var y1: Float = 0f
    private var x2: Float = 0f
    private var y2: Float = 0f

    private var referentialData = ReferentialData(false, 0f, 1f, 0.0, 0.0)
        set(value) {
            field = value
            invalidate()
        }

    override fun onReferentialChanged(refData: ReferentialData) {
        referentialData = refData
    }

    init {
        setWillNotDraw(false)
        val a = context.obtainStyledAttributes(
                R.style.AreaViewStyle, R.styleable.AreaView)

        mBackgroundColor = a.getColor(
                R.styleable.AreaView_backgroundColor,
                this.mBackgroundColor)

        mStrokeColor = a.getColor(
                R.styleable.AreaView_strokeColor,
                this.mStrokeColor)

        a.recycle()

        paintBackground.style = Paint.Style.FILL
        paintBackground.color = mBackgroundColor
        paintBackground.isAntiAlias = true

        val metrics = resources.displayMetrics
        strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_STROKE_WIDTH_DP.toFloat(), metrics)

        paintStroke.style = Paint.Style.STROKE
        paintStroke.color = mStrokeColor
        paintStroke.strokeWidth = strokeWidth
        paintStroke.strokeJoin = Paint.Join.ROUND
        paintStroke.strokeCap = Paint.Cap.ROUND
    }

    fun updateArea(x1: Float, y1: Float, x2: Float, y2: Float) {
        this.x1 = x1
        this.y1 = y1
        this.x2 = x2
        this.y2 = y2

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val scale = referentialData.scale
        canvas.scale(scale, scale)
        paintStroke.strokeWidth = strokeWidth / scale
        canvas.drawLine(x1, y1, x2, y1, paintStroke)
        canvas.drawLine(x1, y1, x1, y2, paintStroke)
        canvas.drawLine(x2, y2, x2, y1, paintStroke)
        canvas.drawLine(x2, y2, x1, y2, paintStroke)
        canvas.drawRect(x1, y1, x2, y2, paintBackground)
        super.onDraw(canvas)
    }
}

private const val DEFAULT_STROKE_WIDTH_DP = 1