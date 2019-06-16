package com.peterlaurence.mapview.markers

import android.content.Context
import android.view.View
import android.view.ViewGroup

class MarkerLayout(context: Context) : ViewGroup(context) {

    private var mScale = 1f

    fun setScale(scale: Float) {
        mScale = scale
        refreshPositions()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            populateLayoutParams(child)
        }
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
        val availableHeight = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(availableWidth, availableHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val layoutParams = child.layoutParams as MarkerLayoutParams
                child.layout(layoutParams.mLeft, layoutParams.mTop, layoutParams.mRight, layoutParams.mBottom)
            }
        }
    }

    private fun populateLayoutParams(child: View): MarkerLayoutParams {
        val layoutParams = child.layoutParams as MarkerLayoutParams
        if (child.visibility != View.GONE) {
            // actual sizes of children
            val actualWidth = child.measuredWidth
            val actualHeight = child.measuredHeight
            // calculate combined anchor offsets
            val widthOffset = actualWidth * layoutParams.relativeAnchorX + layoutParams.absoluteAnchorX
            val heightOffset = actualHeight * layoutParams.relativeAnchorY + layoutParams.absoluteAnchorY
            // get offset position
            val scaledX = (layoutParams.x * mScale).toInt()
            val scaledY = (layoutParams.y * mScale).toInt()
            // save computed values
            layoutParams.mLeft = (scaledX + widthOffset).toInt()
            layoutParams.mTop = (scaledY + heightOffset).toInt()
            layoutParams.mRight = layoutParams.mLeft + actualWidth
            layoutParams.mBottom = layoutParams.mTop + actualHeight
        }
        return layoutParams
    }

    private fun refreshPositions() {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val layoutParams = populateLayoutParams(child)
                child.left = layoutParams.mLeft
                child.top = layoutParams.mTop
                child.right = layoutParams.mRight
                child.bottom = layoutParams.mBottom
            }
        }
    }

    fun addMarker(view: View, left: Int, top: Int, relativeAnchorLeft: Float = -0.5f,
                  relativeAnchorTop: Float = -1f, absoluteAnchorLeft: Float = 0f,
                  absoluteAnchorTop: Float = 0f) {
        val layoutParams = MarkerLayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                left, top,
                relativeAnchorLeft, relativeAnchorTop,
                absoluteAnchorLeft, absoluteAnchorTop)
        addView(view, layoutParams)
    }

    fun removeMarker(view: View) {
        if (view.parent === this) {
            removeView(view)
        }
    }

    fun moveMarker(view: View, left: Int, top: Int) {
        val lp = view.layoutParams as? MarkerLayoutParams ?: return
        lp.x = left
        lp.y = top
        populateLayoutParams(view)
        view.left = lp.mLeft
        view.top = lp.mTop
    }
}

private class MarkerLayoutParams(width: Int, height: Int, var x: Int, var y: Int, var relativeAnchorX: Float, var relativeAnchorY: Float, var absoluteAnchorX: Float, var absoluteAnchorY: Float) : ViewGroup.LayoutParams(width, height) {

    var mTop: Int = 0
    var mLeft: Int = 0
    var mBottom: Int = 0
    var mRight: Int = 0
}