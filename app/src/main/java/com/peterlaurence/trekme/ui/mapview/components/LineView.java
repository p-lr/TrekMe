package com.peterlaurence.trekme.ui.mapview.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import org.jetbrains.annotations.NotNull;

import ovh.plrapps.mapview.ReferentialData;
import ovh.plrapps.mapview.ReferentialListener;


/**
 * A custom view that draws a line between two points and represents the distance
 * between them.
 *
 * @author P.Laurence on 21/06/17.
 */
public class LineView extends View implements ReferentialListener {
    private static final int DEFAULT_STROKE_COLOR = 0xCC311B92;
    private static final int DEFAULT_STROKE_WIDTH_DP = 4;
    private float mStrokeWidth;
    private Paint mPaint = new Paint();
    private float[] mLine = new float[4];

    private ReferentialData mReferentialData;

    @Override
    public void onReferentialChanged(@NotNull ReferentialData referentialData) {
        mReferentialData = referentialData;
        invalidate();
    }

    public LineView(Context context, ReferentialData rd) {
        this(context, rd, null);
    }

    public LineView(Context context, ReferentialData rd, Integer color) {
        super(context);
        setWillNotDraw(false);
        mReferentialData = rd;

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mStrokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_STROKE_WIDTH_DP, metrics);

        if (color == null) {
            mPaint.setColor(DEFAULT_STROKE_COLOR);
        } else {
            mPaint.setColor(color);
        }

        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void updateLine(float x1, float y1, float x2, float y2) {
        mLine[0] = x1;
        mLine[1] = y1;
        mLine[2] = x2;
        mLine[3] = y2;

        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        float scale = mReferentialData.getScale();

        if (mReferentialData.getRotationEnabled()) {
            canvas.rotate(mReferentialData.getAngle(), (float) (getWidth() * mReferentialData.getCenterX()),
                    (float) (getHeight() * mReferentialData.getCenterY()));
        }

        canvas.scale(scale, scale);
        mPaint.setStrokeWidth(mStrokeWidth / scale);
        canvas.drawLine(mLine[0], mLine[1], mLine[2], mLine[3], mPaint);
        super.onDraw(canvas);
    }
}
