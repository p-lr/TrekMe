package com.peterlaurence.trekme.ui.mapview.components

import android.content.Context
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedVectorDrawable
import androidx.appcompat.widget.AppCompatImageView

import com.peterlaurence.trekme.R

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

    init {
        setImageDrawable(mInAnimation)
    }

    fun morphOut(animationEndCallback: Animatable2.AnimationCallback) {
        if (mCurrentDrawable === mInAnimation) {
            mCurrentDrawable = mOutAnimation
            setImageDrawable(mOutAnimation)
            mOutAnimation.registerAnimationCallback(animationEndCallback)
            mOutAnimation.start()

        } else if (mCurrentDrawable === mOutAnimation) {
            mOutAnimation.stop()
        }
    }

    fun morphIn() {
        if (mCurrentDrawable != null) {
            mCurrentDrawable!!.stop()
        }
        mCurrentDrawable = mInAnimation
        mInAnimation.start()
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
