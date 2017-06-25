package com.peterlaurence.trekadvisor.menu.mapview.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

/**
 * A custom view that draws a line between two {@link DistanceMarker} and represents the distance
 * between them.
 *
 * @author peterLaurence on 21/06/17.
 */
public class DistanceView extends View {
    private static final int DEFAULT_STROKE_COLOR = 0xCC311B92;
    private static final int DEFAULT_STROKE_WIDTH_DP = 4;
    private float mStrokeWidth;
    private Paint mPaint = new Paint();
    private float[] mLine = new float[4];

    private float mScale = 1;

    public DistanceView(Context context) {
        super(context);
        setWillNotDraw(false);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mStrokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_STROKE_WIDTH_DP, metrics);

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(DEFAULT_STROKE_COLOR);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setScale(float scale) {
        mScale = scale;
        invalidate();
    }

    public void updateLine(int x1, int y1, int x2, int y2) {
        mLine[0] = x1;
        mLine[1] = y1;
        mLine[2] = x2;
        mLine[3] = y2;

        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.scale(mScale, mScale);
        mPaint.setStrokeWidth(mStrokeWidth / mScale);
        canvas.drawLines(mLine, mPaint);
        super.onDraw(canvas);
    }
}
