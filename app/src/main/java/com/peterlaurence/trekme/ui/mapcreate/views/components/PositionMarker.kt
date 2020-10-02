package com.peterlaurence.trekme.ui.mapcreate.views.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.peterlaurence.trekme.R

/**
 * Custom marker for indicating the current position.
 *
 * @author P.Laurence on 30/06/2019
 */
class PositionMarker(context: Context): View(context) {
    private var measureDimension: Int = 65
    private var positionDimension: Int = 20
    private var backgroundCircleDimension: Int = 60
    private var positionColor = Color.BLUE
    private var positionBackgroundColor = Color.TRANSPARENT

    private val bitmap: Bitmap

    init {
        setStyle()
        bitmap = prepareBitmap()
    }

    private fun setStyle() {
        val a = context.obtainStyledAttributes(
                R.style.PositionMarkerStyle, R.styleable.PositionMarker)

        measureDimension = a.getDimensionPixelSize(R.styleable.PositionMarker_measureDimension, measureDimension)
        positionDimension = a.getDimensionPixelSize(R.styleable.PositionMarker_positionDimension, positionDimension)
        backgroundCircleDimension = a.getDimensionPixelSize(R.styleable.PositionMarker_backgroundCircleDimension, backgroundCircleDimension)
        positionColor = a.getColor(R.styleable.PositionMarker_positionColor, positionColor)
        positionBackgroundColor = a.getColor(R.styleable.PositionMarker_positionBackgroundColor, positionBackgroundColor)


        a.recycle()
    }

    private fun prepareBitmap(): Bitmap {
        /* Paint for the position circle and the arrow */
        val positionPaint = Paint()
        positionPaint.color = positionColor
        positionPaint.isAntiAlias = true

        /* Paint for the background circle */
        val positionBackgroundPaint = Paint()
        positionBackgroundPaint.color = positionBackgroundColor
        positionBackgroundPaint.isAntiAlias = true

        /* Prepare the bitmap */
        val bitmap = Bitmap.createBitmap(measureDimension, measureDimension, Bitmap.Config.ARGB_4444)
        val c = Canvas(bitmap)
        c.drawCircle(measureDimension / 2f, measureDimension / 2f, backgroundCircleDimension / 2f, positionBackgroundPaint)
        c.drawCircle(measureDimension / 2f, measureDimension / 2f, positionDimension / 2f, positionPaint)
        return bitmap
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawBitmap(bitmap, 0f, 0f, null)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measureDimension, measureDimension)
    }
}