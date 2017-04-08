package com.peterlaurence.trekadvisor.menu.mapview.components;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.view.View;

import com.peterlaurence.trekadvisor.R;

/**
 * @author peterLaurence on 08/04/17.
 */
public class MovableMarker extends android.support.v7.widget.AppCompatImageView {
    private boolean isLocationState = false;

    private AnimatedVectorDrawable mCurrentAnimation;
    private AnimatedVectorDrawable mRounded;
    private AnimatedVectorDrawable mLocationToRounded;
    private AnimatedVectorDrawable mRoundedToLocation;


    public MovableMarker(Context context) {
        super(context);

        mRounded = (AnimatedVectorDrawable) context.getDrawable(R.drawable.avd_marker_rounded);
        mLocationToRounded = (AnimatedVectorDrawable) context.getDrawable(R.drawable.avd_marker_location_rounded);
        mRoundedToLocation = (AnimatedVectorDrawable) context.getDrawable(R.drawable.avd_marker_rounded_location);

        mCurrentAnimation = mRounded;
        setImageDrawable(mRounded);
        mRounded.start();

        setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                morph();
            }
        });
    }


    public void morph() {
        if (mCurrentAnimation.isRunning()) {
            mCurrentAnimation.stop();
        }

        AnimatedVectorDrawable newDrawable = isLocationState ? mLocationToRounded : mRoundedToLocation;
        mCurrentAnimation = newDrawable;
        setImageDrawable(newDrawable);
        newDrawable.start();
        isLocationState = !isLocationState;
    }
}
