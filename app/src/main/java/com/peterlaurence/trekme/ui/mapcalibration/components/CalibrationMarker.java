package com.peterlaurence.trekme.ui.mapcalibration.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import com.peterlaurence.trekme.R;

/**
 * Custom marker for map calibration.
 *
 * @author P.Laurence on 14/05/16.
 */
public class CalibrationMarker extends View {
    private int mMeasureDimension;
    private int mLineWidth;
    private int mSightColor = Color.BLUE;
    private int mSightBackgroundColor = Color.BLUE;

    private Paint mPaint;
    private Paint mPaintBackground;

    private double mRelativeX;
    private double mRelativeY;

    public CalibrationMarker(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        final TypedArray a = context.obtainStyledAttributes(
                R.style.CalibrationMarkerStyle, R.styleable.CalibrationMarker);

        mMeasureDimension = a.getDimensionPixelSize(
                R.styleable.CalibrationMarker_measureDimension,
                200);

        mLineWidth = a.getDimensionPixelSize(
                R.styleable.CalibrationMarker_lineWidth,
                10);

        mSightColor = a.getColor(
                R.styleable.CalibrationMarker_sightColor,
                mSightColor);

        mSightBackgroundColor = a.getColor(
                R.styleable.CalibrationMarker_sightBackgroundColor,
                mSightBackgroundColor);

        a.recycle();

        /* Paint for the sight */
        mPaint = new Paint();
        mPaint.setColor(mSightColor);
        mPaint.setStrokeWidth(mLineWidth);
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
        canvas.drawLine(0, mMeasureDimension / 2, mMeasureDimension / 2 - mLineWidth / 2, mMeasureDimension / 2, mPaint);
        canvas.drawLine(mMeasureDimension / 2 + mLineWidth / 2, mMeasureDimension / 2, mMeasureDimension, mMeasureDimension / 2, mPaint);
        canvas.drawLine(mMeasureDimension / 2, 0, mMeasureDimension / 2, mMeasureDimension / 2 - mLineWidth / 2, mPaint);
        canvas.drawLine(mMeasureDimension / 2, mMeasureDimension / 2 + mLineWidth / 2, mMeasureDimension / 2, mMeasureDimension, mPaint);
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mMeasureDimension, mMeasureDimension);
    }

    public double getRelativeX() {
        return mRelativeX;
    }

    public void setRelativeX(double relativeX) {
        mRelativeX = relativeX;
    }

    public double getRelativeY() {
        return mRelativeY;
    }

    public void setRelativeY(double relativeY) {
        mRelativeY = relativeY;
    }
}
