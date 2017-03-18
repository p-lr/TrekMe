package com.peterlaurence.trekadvisor.menu.mapview.components;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import java.util.Arrays;
import java.util.HashSet;

/**
 * This is a modified version of {@link com.qozix.tileview.paths.CompositePathView}, using
 * {@code Canvas.drawLines} to draw a path. This method is much more efficient as it's
 * hardware accelerated, although the result is not as neat as the original implementation (which
 * uses {@link Path}.
 *
 * @author peterLaurence on 19/02/17
 */
public class PathView extends View {

    private static final int DEFAULT_STROKE_COLOR = 0xCC311B92;
    private static final int DEFAULT_STROKE_WIDTH_DP = 4;
    private float mStrokeWidthDefault;

    private float mScale = 1;

    private boolean mShouldDraw = true;

    private HashSet<DrawablePath> mDrawablePaths = new HashSet<>();

    private Paint mDefaultPaint = new Paint();


    public PathView(Context context) {
        super(context);
        setWillNotDraw(false);

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        mStrokeWidthDefault = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_STROKE_WIDTH_DP, metrics);

        mDefaultPaint.setStyle(Paint.Style.STROKE);
        mDefaultPaint.setColor(DEFAULT_STROKE_COLOR);
        mDefaultPaint.setStrokeWidth(mStrokeWidthDefault);
        mDefaultPaint.setAntiAlias(true);
        mDefaultPaint.setStrokeJoin(Paint.Join.ROUND);
        mDefaultPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public float getScale() {
        return mScale;
    }

    public void setScale(float scale) {
        mScale = scale;
        invalidate();
    }

    public Paint getDefaultPaint() {
        return mDefaultPaint;
    }

    public void addPath(DrawablePath drawablePath) {
        if (drawablePath.paint == null) {
            drawablePath.paint = mDefaultPaint;
            drawablePath.width = mStrokeWidthDefault;
        }

        mDrawablePaths.add(drawablePath);
        invalidate();
    }

    public void removePath(DrawablePath path) {
        mDrawablePaths.remove(path);
        invalidate();
    }

    public void clear() {
        mDrawablePaths.clear();
        invalidate();
    }

    public void setShouldDraw(boolean shouldDraw) {
        mShouldDraw = shouldDraw;
        invalidate();
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mShouldDraw) {
            canvas.scale(mScale, mScale);
            for (DrawablePath drawablePath : mDrawablePaths) {
                if (drawablePath.visible) {
                    drawablePath.paint.setStrokeWidth(drawablePath.width / mScale);
                    canvas.drawLines(drawablePath.path, drawablePath.paint);
                }
            }
        }
        super.onDraw(canvas);
    }

    public static class DrawablePath {

        public DrawablePath(float[] path, Paint paint) {
            this.path = path;
            this.paint = paint;
            visible = true;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(path);
        }

        /**
         * The path that this drawable will follow.
         */
        public float[] path;

        /**
         * The paint to be used for this path.
         */
        public Paint paint;

        /**
         * Whether or not this path shall be drawn
         */
        public boolean visible;

        /**
         * The width of the path
         */
        public float width;
    }
}