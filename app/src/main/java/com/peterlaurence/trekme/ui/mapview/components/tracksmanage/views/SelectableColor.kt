package com.peterlaurence.trekme.ui.mapview.components.tracksmanage.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.peterlaurence.trekme.util.px


class SelectableColor(context: Context) : View(context) {
    private val diameterNormal: Int = 10.px
    private val diameterSelected: Int = 12.px
    private var diameter: Int = diameterNormal
    private val paint: Paint = Paint()
    private val strokePaint = Paint()

    init {
        paint.color = Color.BLUE
        paint.isAntiAlias = true
        strokePaint.isAntiAlias = true
        strokePaint.style = Paint.Style.STROKE
        strokePaint.color = Color.BLACK
        strokePaint.strokeWidth = 3.px.toFloat()
    }

    fun setColor(color: Int) {
        paint.color = color
        invalidate()
    }

    override fun setSelected(selected: Boolean) {
        diameter = if (selected) diameterSelected else diameterNormal
        super.setSelected(selected)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        val radius = (diameter / 2).toFloat()
        canvas.drawCircle(radius, radius, radius, paint)
        if (isSelected) {
            canvas.drawCircle(radius, radius, radius, strokePaint)
        }
        canvas.restore()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(diameterSelected, diameterSelected)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}