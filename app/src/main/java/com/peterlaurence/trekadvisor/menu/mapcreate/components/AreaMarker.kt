package com.peterlaurence.trekadvisor.menu.mapcreate.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.peterlaurence.trekadvisor.R


/**
 * Custom marker which is part of the area selection feature.
 *
 * @author peterLaurence on 11/05/18.
 */
class AreaMarker(context: Context) : View(context) {
    private var mMeasureDimension: Int = 0
    private var mBackgroundColor = Color.BLUE
    private val mPaintBackground: Paint = Paint()

    init {
        val a = context.obtainStyledAttributes(
                R.style.AreaMarkerStyle, R.styleable.AreaMarker)

        mMeasureDimension = a.getDimensionPixelSize(
                R.styleable.AreaMarker_measureDimension,
                200)

        mBackgroundColor = a.getColor(
                R.styleable.AreaMarker_sightBackgroundColor,
                mBackgroundColor)

        a.recycle()

        /* Paint for the background circle */
        mPaintBackground.color = mBackgroundColor
        mPaintBackground.isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        canvas.drawCircle((mMeasureDimension / 2).toFloat(), (mMeasureDimension / 2).toFloat(),
                (mMeasureDimension / 2).toFloat(), mPaintBackground)
        canvas.restore()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(mMeasureDimension, mMeasureDimension)
    }
}