package com.peterlaurence.trekadvisor.menu.mapview.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    private int mOrientationRadius1Dimension;
    private int mPositionColor = Color.BLUE;
    private int mOrientationColor = Color.BLUE;
    private int mPositionBackGroundColor = Color.TRANSPARENT;

    private Bitmap mBitmap;

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

        int positionDimension = a.getDimensionPixelSize(
                R.styleable.PositionOrientationMarker_positionDimension,
                20);

        mOrientationRadius1Dimension = a.getDimensionPixelSize(
                R.styleable.PositionOrientationMarker_orientationRadius1Dimension,
                mOrientationRadius1Dimension);

        int backgroundCircleDimension = a.getDimensionPixelSize(
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
        Paint positionPaint = new Paint();
        positionPaint.setColor(mPositionColor);
        positionPaint.setAntiAlias(true);

        /* Paint for the background circle */
        Paint positionBackgroundPaint = new Paint();
        positionBackgroundPaint.setColor(mPositionBackGroundColor);
        positionBackgroundPaint.setAntiAlias(true);

        /* Path for the orientation arrow */
        Path path = new Path();
        float delta = (mMeasureDimension - mOrientationRadius1Dimension) / 2;
        path.arcTo(delta, delta,
                mOrientationRadius1Dimension + delta, mOrientationRadius1Dimension + delta, -90, orientationAngle, false);
        path.lineTo(mMeasureDimension / 2, 0);
        path.arcTo(delta, delta,
                mOrientationRadius1Dimension + delta, mOrientationRadius1Dimension + delta, -90, -orientationAngle, false);
        path.lineTo(mMeasureDimension / 2, 0);

        /* Prepare the bitmap */
        mBitmap = Bitmap.createBitmap(mMeasureDimension, mMeasureDimension, Bitmap.Config.ARGB_4444);
        Canvas c = new Canvas(mBitmap);
        c.drawCircle(mMeasureDimension / 2, mMeasureDimension / 2, backgroundCircleDimension / 2, positionBackgroundPaint);
        c.drawCircle(mMeasureDimension / 2, mMeasureDimension / 2, positionDimension / 2, positionPaint);
        c.drawPath(path, positionPaint);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.rotate(mAzimuth, mMeasureDimension / 2, mMeasureDimension / 2);
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mMeasureDimension, mMeasureDimension);
    }

    @Override
    public void onOrientation(int azimuth) {
        if (Math.abs(azimuth - mAzimuth) > 0.5) {
            mAzimuth = azimuth;
            invalidate();
        }
    }
}
