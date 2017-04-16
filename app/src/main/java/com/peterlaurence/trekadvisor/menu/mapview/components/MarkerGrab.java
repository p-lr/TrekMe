package com.peterlaurence.trekadvisor.menu.mapview.components;

import android.content.Context;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.support.v7.widget.AppCompatImageView;

import com.peterlaurence.trekadvisor.R;

/**
 * An marker meant to cast its move so it can be used to move other views that are e.g to small to
 * be dragged easily.
 *
 * @author peterLaurence on 10/04/17.
 */
public class MarkerGrab extends AppCompatImageView {
    private AnimatedVectorDrawable mOutAnimation;
    private AnimatedVectorDrawable mInAnimation;
    private AnimatedVectorDrawable mCurrentDrawable;

    public MarkerGrab(Context context) {
        super(context);

        mOutAnimation = (AnimatedVectorDrawable) context.getDrawable(R.drawable.avd_marker_circle_grab_out);
        mInAnimation = (AnimatedVectorDrawable) context.getDrawable(R.drawable.avd_marker_circle_grab_in);

        setImageDrawable(mInAnimation);
    }

    public void morphOut(Animatable2.AnimationCallback animationEndCallback) {
        if (mCurrentDrawable == mInAnimation) {
            mCurrentDrawable = mOutAnimation;
            setImageDrawable(mOutAnimation);
            mOutAnimation.registerAnimationCallback(animationEndCallback);
            mOutAnimation.start();

        } else if (mCurrentDrawable == mOutAnimation) {
            mOutAnimation.stop();
        }

    }

    public void morphIn() {
        if (mCurrentDrawable != null) {
            mCurrentDrawable.stop();
        }
        mCurrentDrawable = mInAnimation;
        mInAnimation.start();
    }
}
