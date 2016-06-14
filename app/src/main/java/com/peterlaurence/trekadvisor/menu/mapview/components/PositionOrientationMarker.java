package com.peterlaurence.trekadvisor.menu.mapview.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import com.peterlaurence.trekadvisor.R;
import com.peterlaurence.trekadvisor.core.sensors.OrientationSensor;

/**
 * Custom marker for indicating the current position, and optionally the orientation.
 *
 * @author peterLaurence on 03/04/16.
 */
public class PositionOrientationMarker extends View implements OrientationSensor.OrientationListener{
    private int mMeasureDimension;
    private int mPositionDimension;
    private int mOrientationRadius1Dimension;
    private int mBackgroundCircleDimension;
    private int mPositionColor = Color.BLUE;
    private int mOrientationColor = Color.BLUE;
    private int mPositionBackGroundColor = Color.TRANSPARENT;

    private Paint mPositionPaint;
    private Paint mPositionBackgroundPaint;
    private Path mPath;

    private int mAzimuth;

    public PositionOrientationMarker(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        final TypedArray a = context.obtainStyledAttributes(
                R.style.PositionMarkerStyle, R.styleable.PositionOrientationMarker);

        mMeasureDimension = a.getDimensionPixelSize(
                R.styleable.PositionOrientationMarker_mesureDimension,
                65);

        mPositionDimension = a.getDimensionPixelSize(
                R.styleable.PositionOrientationMarker_positionDimension,
                20);

        mOrientationRadius1Dimension = a.getDimensionPixelSize(
                R.styleable.PositionOrientationMarker_orientationRadius1Dimension,
                mOrientationRadius1Dimension);

        mBackgroundCircleDimension = a.getDimensionPixelSize(
                R.styleable.PositionOrientationMarker_backgroundCircleDimension,
                60);

        int orientationAngle = a.getInt(
                R.styleable.PositionOrientationMarker_orientationAngle,
                40);

        mPositionColor = a.getColor(
                R.styleable.PositionOrientationMarker_positionColor,
                mPositionColor);

        mPositionBackGroundColor = a.getColor(
                R.styleable.PositionOrientationMarker_positionBackgroundColor,
                mPositionBackGroundColor);

        mOrientationColor = a.getColor(
                R.styleable.PositionOrientationMarker_orientationColor,
                mOrientationColor);

        a.recycle();

        /* Paint for the position circle and the arrow */
        mPositionPaint = new Paint();
        mPositionPaint.setColor(mPositionColor);
        mPositionPaint.setAntiAlias(true);
//        mPositionPaint.setStrokeWidth(2f);

        /* Paint for the background circle */
        mPositionBackgroundPaint = new Paint();
        mPositionBackgroundPaint.setColor(mPositionBackGroundColor);
        mPositionBackgroundPaint.setAntiAlias(true);

        /* Path for the orientation arrow */
        mPath = new Path();
        float delta = (mMeasureDimension - mOrientationRadius1Dimension) / 2;
        mPath.arcTo(delta, delta,
                mOrientationRadius1Dimension + delta, mOrientationRadius1Dimension + delta, -90, orientationAngle, false);
        mPath.lineTo(mMeasureDimension / 2, 0);
        mPath.arcTo(delta, delta,
                mOrientationRadius1Dimension + delta, mOrientationRadius1Dimension + delta, -90, -orientationAngle, false);
        mPath.lineTo(mMeasureDimension / 2, 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.rotate(mAzimuth, mMeasureDimension / 2, mMeasureDimension / 2);
        canvas.drawCircle(mMeasureDimension / 2, mMeasureDimension / 2, mBackgroundCircleDimension / 2, mPositionBackgroundPaint);
        canvas.drawCircle(mMeasureDimension / 2, mMeasureDimension / 2, mPositionDimension / 2, mPositionPaint);
        canvas.drawPath(mPath, mPositionPaint);
        canvas.restore();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mMeasureDimension, mMeasureDimension);
    }

    @Override
    public void onOrientation(int azimuth) {
        mAzimuth = azimuth;
        invalidate();
    }
}
