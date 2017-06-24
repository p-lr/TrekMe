package com.peterlaurence.trekadvisor.menu.mapview.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import com.peterlaurence.trekadvisor.R;

/**
 * Custom marker to show the distance with another view.
 *
 * @author peterLaurence on 01/04/17.
 */
public class DistanceMarker extends View {
    private int mMeasureDimension;
    private int mSightColor = Color.BLUE;
    private int mSightBackgroundColor = Color.BLUE;

    private Paint mPaint;
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

        int lineWidth = a.getDimensionPixelSize(
                R.styleable.DistanceMarker_lineWidth,
                10);

        mSightColor = a.getColor(
                R.styleable.DistanceMarker_sightColor,
                mSightColor);

        mSightBackgroundColor = a.getColor(
                R.styleable.DistanceMarker_sightBackgroundColor,
                mSightBackgroundColor);

        a.recycle();

        /* Paint for the sight */
        mPaint = new Paint();
        mPaint.setColor(mSightColor);
        mPaint.setStrokeWidth(lineWidth);
        mPaint.setAntiAlias(true);

        /* Paint for the background circle */
        mPaintBackground = new Paint();
        mPaintBackground.setColor(mSightBackgroundColor);
        mPaintBackground.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.drawCircle(mMeasureDimension / 2, mMeasureDimension / 2, mMeasureDimension / 2, mPaintBackground);
        canvas.drawLine(0, mMeasureDimension / 2, mMeasureDimension, mMeasureDimension / 2, mPaint);
        canvas.drawLine(mMeasureDimension / 2, 0, mMeasureDimension / 2, mMeasureDimension, mPaint);
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mMeasureDimension, mMeasureDimension);
    }
}
