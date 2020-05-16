package com.peterlaurence.trekme.ui.mapview.components

import android.content.Context
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import androidx.appcompat.widget.AppCompatImageView

import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.ui.tools.TouchMoveListener

/**
 * An marker meant to cast its move so it can be used to move other views that are e.g to small to
 * be dragged easily.
 *
 * @author peterLaurence on 10/04/17 -- Converted to Kotlin on 24/02/2019
 */
class MarkerGrab(context: Context) : AppCompatImageView(context) {
    private val mOutAnimation: AnimatedVectorDrawable = context.getDrawable(R.drawable.avd_marker_circle_grab_out) as AnimatedVectorDrawable
    private val mInAnimation: AnimatedVectorDrawable = context.getDrawable(R.drawable.avd_marker_circle_grab_in) as AnimatedVectorDrawable
    private var mCurrentDrawable: AnimatedVectorDrawable? = null

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

    init {
        setImageDrawable(mInAnimation)
    }

    fun morphOut(animationEndCallback: Animatable2.AnimationCallback) {
        if (mCurrentDrawable === mInAnimation) {
            mCurrentDrawable = mOutAnimation
            setImageDrawable(mOutAnimation)
            mOutAnimation.registerAnimationCallback(animationEndCallback)
            mOutAnimation.start()
        }
    }

    fun morphIn() {
        if (mCurrentDrawable === mOutAnimation) {
            mOutAnimation.clearAnimationCallbacks()
        }
        mCurrentDrawable = mInAnimation
        setImageDrawable(mInAnimation)
        mInAnimation.start()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
