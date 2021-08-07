package com.peterlaurence.trekme.ui.mapview.components

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.peterlaurence.trekme.ui.tools.TouchMoveListener
import com.peterlaurence.trekme.util.dpToPx

/**
 * A marker with a circle shape, and which offers [morphIn] and [morphOut] methods to respectively
 * animate the appearing and disappearing of the marker.
 * At first creation, this marker has diameter of 0 - so is not visible.
 *
 * @author P.Laurence on 18/05/2020
 */
class MarkerGrab @JvmOverloads constructor(context: Context, private val fullSized: Int = dpToPx(100f).toInt()) : View(context) {
    private var paint: Paint = Paint().apply {
        color = Color.parseColor("#55448AFF")
        isAntiAlias = true
    }
        set(value) {
            field = value
            invalidate()
        }

    private var diameter: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    /* A simple wrapper just for the Animation framework */
    @Suppress("unused")
    private val wrapper = object {
        fun setDiameter(d: Int) {
            diameter = d
        }

        fun getDiameter(): Int = diameter
    }

    /**
     * When the setter is called, we keep a reference on the [TouchMoveListener].
     * It's useful when we later need to retrieve this reference, typically to unregister it as a
     * listener. In situations where keeping this reference is useless, using [setOnTouchListener]
     * on the [MarkerGrab] instance is enough.
     */
    var onTouchMoveListener: TouchMoveListener? = null
        set(value) {
            setOnTouchListener(value)
            field = value
        }

    fun morphIn(animatorListener: Animator.AnimatorListener? = null) {
        ObjectAnimator.ofInt(wrapper, "diameter", 0, fullSized).apply {
            duration = 500
            if (animatorListener != null) {
                addListener(animatorListener)
            }
            start()
        }
    }

    fun morphOut(animatorListener: Animator.AnimatorListener? = null) {
        ObjectAnimator.ofInt(wrapper, "diameter", 0).apply {
            duration = 500
            if (animatorListener != null) {
                addListener(animatorListener)
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.drawCircle(fullSized / 2.toFloat(), fullSized / 2.toFloat(), diameter / 2.toFloat(), paint)
        canvas.restore()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(fullSized, fullSized)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}