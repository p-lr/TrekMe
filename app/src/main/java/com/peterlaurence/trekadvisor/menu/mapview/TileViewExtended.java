package com.peterlaurence.trekadvisor.menu.mapview;

import android.content.Context;
import android.graphics.Paint;
import android.view.MotionEvent;

import com.peterlaurence.trekadvisor.menu.mapview.components.PathView;
import com.qozix.tileview.TileView;
import com.qozix.tileview.geom.CoordinateTranslater;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * Specialization of TileView to allow custom custom control of touch events.
 *
 * @author peterLaurence on 28/03/16.
 */
public class TileViewExtended extends TileView {

    private WeakReference<SingleTapStaticListener> mSingleTapListenerWeakReference;
    private boolean mScrollingAtStart;

    private WeakReference<ScrollListener> mScrollListenerWeakReference;
    private PathView mPathView;

    public TileViewExtended(Context context) {
        super(context);

        mPathView = new PathView(context);
        addView(mPathView);
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {
        mScrollingAtStart = !getScroller().isFinished();

        return super.onDown(motionEvent);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
        if (mSingleTapListenerWeakReference != null) {
            SingleTapStaticListener singleTapListener = mSingleTapListenerWeakReference.get();
            if (singleTapListener != null && !mScrollingAtStart) {
                singleTapListener.onSingleTapStatic();
            }
        }

        return super.onSingleTapConfirmed(motionEvent);
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        if (mScrollListenerWeakReference != null) {
            ScrollListener scrollListener = mScrollListenerWeakReference.get();
            if (scrollListener != null) {
                scrollListener.onScroll();
            }
        }

        return super.onScroll(motionEvent, motionEvent1, v, v1);
    }

    /**
     * On a tap up, if the {@link TileView} was not scrolling when the touch down occurred, we
     * consider that event eligible for a SingleTapStatic event.
     */
    public interface SingleTapStaticListener {
        void onSingleTapStatic();
    }

    public void setSingleTapListener(SingleTapStaticListener listener) {
        mSingleTapListenerWeakReference = new WeakReference<>(listener);
    }

    public interface ScrollListener {
        void onScroll();
    }

    public void setScrollListener(ScrollListener listener) {
        mScrollListenerWeakReference = new WeakReference<>(listener);
    }

    /**
     * An alternative way to {@link #drawPath(List, Paint)}, which uses a {@link PathView}.
     *
     * @param positions {@link List} of coordinates, typically projected values
     * @param paint     The Paint instance that defines the style of the drawn path.
     * @return The {@link PathView.DrawablePath} instance passed to the TileView.
     */
    public PathView.DrawablePath drawPathQuickly(List<double[]> positions, Paint paint) {
        CoordinateTranslater mCoordinateTranslater = getCoordinateTranslater();

        int size;
        if (positions.size() > 2) {
            size = 4 * (positions.size() - 2) + 4;
        } else {
            size = 2 * positions.size();
        }
        float[] path = new float[size];
        int i = 0;
        for (double[] point : positions) {
            if (i == 0) {
                path[0] = (float) mCoordinateTranslater.translateX(point[0]);
                path[1] = (float) mCoordinateTranslater.translateY(point[1]);
                i += 2;
            } else {
                path[i] = (float) mCoordinateTranslater.translateX(point[0]);
                path[i + 1] = (float) mCoordinateTranslater.translateY(point[1]);
                if (i + 2 == path.length) break;
                path[i + 2] = path[i];
                path[i + 3] = path[i + 1];
                i += 4;
            }
        }
        return mPathView.addPath(path, paint);
    }
}
