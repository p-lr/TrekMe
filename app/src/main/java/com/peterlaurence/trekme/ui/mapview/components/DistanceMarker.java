package com.peterlaurence.trekme.ui.mapview.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import com.peterlaurence.trekme.R;

/**
 * Custom marker which is part of the distance measurement feature.
 *
 * @author P.Laurence on 24/06/17.
 */
public class DistanceMarker extends View {
    private int mMeasureDimension;
    private int mBackgroundColor = Color.BLUE;

    private Paint mPaintBackground;

    public DistanceMarker(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        final TypedArray a = context.obtainStyledAttributes(
                R.style.DistanceMarkerStyle, R.styleable.DistanceMarker);

        mMeasureDimension = a.getDimensionPixelSize(
                R.styleable.DistanceMarker_measureDimension,
                200);

        mBackgroundColor = a.getColor(
                R.styleable.DistanceMarker_sightBackgroundColor,
                mBackgroundColor);

        a.recycle();

        /* Paint for the background circle */
        mPaintBackground = new Paint();
        mPaintBackground.setColor(mBackgroundColor);
        mPaintBackground.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.drawCircle(mMeasureDimension / 2, mMeasureDimension / 2, mMeasureDimension / 2, mPaintBackground);
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mMeasureDimension, mMeasureDimension);
    }
}
