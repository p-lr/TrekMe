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

import org.jetbrains.annotations.NotNull;

import ovh.plrapps.mapview.ReferentialData;
import ovh.plrapps.mapview.ReferentialListener;

/**
 * Custom marker for indicating the current position, and optionally the orientation.
 *
 * @author P.Laurence on 03/04/16.
 */
public class PositionOrientationMarker extends View implements ReferentialListener {
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

    private float mAzimuth;
    private ReferentialData referentialData;

    public PositionOrientationMarker(Context context) {
        super(context);
        init(context);
    }

    @Override
    public void onReferentialChanged(@NotNull ReferentialData referentialData) {
        this.referentialData = referentialData;
    }

    private void init(Context context) {
        final TypedArray a = context.obtainStyledAttributes(
                R.style.PositionMarkerStyle, R.styleable.PositionOrientationMarker);

        mMeasureDimension = a.getDimensionPixelSize(
                R.styleable.PositionOrientationMarker_measureDimension,
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
            float delta = (mMeasureDimension - mOrientationRadius1Dimension) / 2f;
            path.arcTo(delta, delta,
                    mOrientationRadius1Dimension + delta, mOrientationRadius1Dimension + delta, -90, mOrientationAngle, false);
            path.lineTo(mMeasureDimension / 2f, 0);
            path.arcTo(delta, delta,
                    mOrientationRadius1Dimension + delta, mOrientationRadius1Dimension + delta, -90, -mOrientationAngle, false);
            path.lineTo(mMeasureDimension / 2f, 0);
        }

        /* Prepare the bitmap */
        mBitmap = Bitmap.createBitmap(mMeasureDimension, mMeasureDimension, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(mBitmap);
        c.drawCircle(mMeasureDimension / 2f, mMeasureDimension / 2f, mBackgroundCircleDimension / 2f, positionBackgroundPaint);
        c.drawCircle(mMeasureDimension / 2f, mMeasureDimension / 2f, mPositionDimension / 2f, positionPaint);
        if (mOrientationEnabled && path != null) {
            c.drawPath(path, positionPaint);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mOrientationEnabled) {
            float mapRotation = 0f;
            if (referentialData != null) {
                mapRotation = referentialData.getAngle();
            }
            canvas.rotate(mAzimuth + mapRotation, mMeasureDimension / 2f, mMeasureDimension / 2f);
        }

        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mMeasureDimension, mMeasureDimension);
    }

    public void onOrientation(float azimuth) {
        if (Math.abs(azimuth - mAzimuth) > 0.5) {
            mAzimuth = azimuth;
            invalidate();
        }
    }

    public void onOrientationEnable() {
        mOrientationEnabled = true;
        prepareBitmap();
        invalidate();
    }

    public void onOrientationDisable() {
        mOrientationEnabled = false;
        prepareBitmap();
        invalidate();
    }
}
