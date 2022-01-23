package com.peterlaurence.trekme.features.map.presentation.ui.legacy.tracksmanage.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.peterlaurence.trekme.util.dpToPx

/**
 * A circle view. When selected, it grows with a dark-grey stroke.
 *
 * @author P.Laurence on 19/11/20
 */
class SelectableColor @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val radiusNormal: Int = dpToPx(18f).toInt()
    private val radiusSelected: Int = dpToPx(24f).toInt()
    private var radius: Int = radiusNormal
    private val paint: Paint = Paint()
    private val strokePaint = Paint()

    init {
        paint.color = Color.BLUE
        paint.isAntiAlias = true
        strokePaint.isAntiAlias = true
        strokePaint.style = Paint.Style.STROKE
        strokePaint.color = Color.parseColor("#424242")
        strokePaint.strokeWidth = dpToPx(4f)
    }

    fun setColor(color: Int) {
        paint.color = color
        invalidate()
    }

    override fun setSelected(selected: Boolean) {
        radius = if (selected) radiusSelected else radiusNormal
        super.setSelected(selected)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        val radius = radius.toFloat()
        val center = radiusSelected.toFloat()
        canvas.drawCircle(center, center, radius, paint)
        if (isSelected) {
            canvas.drawCircle(center, center, radius - strokePaint.strokeWidth / 2, strokePaint)
        }
        canvas.restore()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val dim = radiusSelected  * 2
        setMeasuredDimension(dim, dim)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}