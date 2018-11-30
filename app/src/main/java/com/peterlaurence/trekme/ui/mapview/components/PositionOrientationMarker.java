package com.peterlaurence.trekme.ui.mapview.components;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.events.OrientationEventManager;

/**
 * Custom marker for indicating the current position, and optionally the orientation.
 *
 * @author peterLaurence on 03/04/16.
 */
public class PositionOrientationMarker extends View implements OrientationEventManager.OrientationListener {
    private int mMeasureDimension;
    private int mOrientationRadius1Dimension;
    private int mPositionDimension;
    private int mBackgroundCircleDimension;
    private int mOrientationAngle;
    private int mPositionColor = Color.BLUE;
    private int mOrientationColor = Color.BLUE;
    private int mPositionBackGroundColor = Color.TRANSPARENT;
    private boolean mOrientationEnabled;

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

        mPositionDimension = a.getDimensionPixelSize(
                R.styleable.PositionOrientationMarker_positionDimension,
                20);

        mOrientationRadius1Dimension = a.getDimensionPixelSize(
                R.styleable.PositionOrientationMarker_orientationRadius1Dimension,
                mOrientationRadius1Dimension);

        mBackgroundCircleDimension = a.getDimensionPixelSize(
                R.styleable.PositionOrientationMarker_backgroundCircleDimension,
                60);

        mOrientationAngle = a.getInt(
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

        prepareBitmap();
    }

    private void prepareBitmap() {
        /* Paint for the position circle and the arrow */
        Paint positionPaint = new Paint();
        positionPaint.setColor(mPositionColor);
        positionPaint.setAntiAlias(true);

        /* Paint for the background circle */
        Paint positionBackgroundPaint = new Paint();
        positionBackgroundPaint.setColor(mPositionBackGroundColor);
        positionBackgroundPaint.setAntiAlias(true);

        /* Path for the orientation arrow */
        Path path = null;
        if (mOrientationEnabled) {
            path = new Path();
            float delta = (mMeasureDimension - mOrientationRadius1Dimension) / 2;
            path.arcTo(delta, delta,
                    mOrientationRadius1Dimension + delta, mOrientationRadius1Dimension + delta, -90, mOrientationAngle, false);
            path.lineTo(mMeasureDimension / 2, 0);
            path.arcTo(delta, delta,
                    mOrientationRadius1Dimension + delta, mOrientationRadius1Dimension + delta, -90, -mOrientationAngle, false);
            path.lineTo(mMeasureDimension / 2, 0);
        }

        /* Prepare the bitmap */
        mBitmap = Bitmap.createBitmap(mMeasureDimension, mMeasureDimension, Bitmap.Config.ARGB_4444);
        Canvas c = new Canvas(mBitmap);
        c.drawCircle(mMeasureDimension / 2, mMeasureDimension / 2, mBackgroundCircleDimension / 2, positionBackgroundPaint);
        c.drawCircle(mMeasureDimension / 2, mMeasureDimension / 2, mPositionDimension / 2, positionPaint);
        if (mOrientationEnabled && path != null) {
            c.drawPath(path, positionPaint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mOrientationEnabled) {
            canvas.rotate(mAzimuth, mMeasureDimension / 2, mMeasureDimension / 2);
        }

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

    @Override
    public void onOrientationEnable() {
        mOrientationEnabled = true;
        prepareBitmap();
        invalidate();
    }

    @Override
    public void onOrientationDisable() {
        mOrientationEnabled = false;
        prepareBitmap();
        invalidate();
    }
}
