package com.peterlaurence.trekme.ui.mapview.components;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.gson.MarkerGson;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * This {@link android.widget.ImageView} has two states :
 * <ul>
 * <li>Static : it appears as a classic "point of interest" marker.</li>
 * <li>Dynamic : it has a round shape with arrows turning around it, indicating that it can
 * be moved.</li>
 * </ul>
 * The transitions between the two states are animated, using animated vector drawables.
 *
 * @author peterLaurence on 08/04/17.
 */
public class MovableMarker extends AppCompatImageView {
    private AnimatedVectorDrawable mRounded;
    private Drawable mStatic;
    private AnimatedVectorDrawable mStaticToDynamic;
    private AnimatedVectorDrawable mDynamicToStatic;

    /* The model object that this view represents */
    private MarkerGson.Marker mMarker;
    private boolean mIsStatic;

    /* The relative coordinates are kept here. Although this shouldn't be a concern of this object,
     * the TileView don't offer the possibility to retrieve the relative coordinates of a marker,
     * so we have to save them in e.g, the marker's view.
     */
    private double relativeX;
    private double relativeY;


    /**
     * The {@code mRounded} drawable is just the end state of the {@code mStaticToDynamic}
     * {@link AnimatedVectorDrawable}.
     * The {@code mStatic} drawable is the end state of the {@code mDynamicToStatic}
     * {@link AnimatedVectorDrawable}. <br>
     */
    public MovableMarker(Context context, boolean staticForm, MarkerGson.Marker marker) {
        super(context);

        mRounded = (AnimatedVectorDrawable) context.getDrawable(R.drawable.avd_marker_rounded);
        mStatic = context.getDrawable(R.drawable.vd_marker_location_rounded);
        mStaticToDynamic = (AnimatedVectorDrawable) context.getDrawable(R.drawable.avd_marker_location_rounded);
        mDynamicToStatic = (AnimatedVectorDrawable) context.getDrawable(R.drawable.avd_marker_rounded_location);

        /* Keep a reference on the model object */
        mMarker = marker;

        /* Init the drawable */
        if (staticForm) {
            initStatic();
        } else {
            initRounded();
        }
    }

    public void initRounded() {
        mIsStatic = false;
        setImageDrawable(mRounded);
        mRounded.start();
    }

    public void initStatic() {
        mIsStatic = true;
        setImageDrawable(mStatic);
    }

    public void morphToStaticForm() {
        if (!mIsStatic) {
            stopCurrentAnimation();
            setImageDrawable(mDynamicToStatic);
            mDynamicToStatic.start();
            mIsStatic = true;
        }
    }

    public void morphToDynamicForm() {
        if (mIsStatic) {
            stopCurrentAnimation();
            setImageDrawable(mStaticToDynamic);
            mStaticToDynamic.start();
            mIsStatic = false;
        }
    }

    private void stopCurrentAnimation() {
        Drawable drawable = getDrawable();
        if (drawable instanceof AnimatedVectorDrawable) {
            ((AnimatedVectorDrawable) drawable).stop();
        }
    }

    public double getRelativeX() {
        return relativeX;
    }

    public void setRelativeX(double relativeX) {
        this.relativeX = relativeX;
    }

    public double getRelativeY() {
        return relativeY;
    }

    public void setRelativeY(double relativeY) {
        this.relativeY = relativeY;
    }

    public MarkerGson.Marker getMarker() {
        return mMarker;
    }
}
