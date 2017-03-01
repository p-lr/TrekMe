package com.peterlaurence.trekadvisor.menu.mapview;

import android.content.Context;
import android.graphics.Paint;
import android.view.MotionEvent;

import com.peterlaurence.trekadvisor.menu.mapview.components.PathView;
import com.qozix.tileview.TileView;

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
     * An alternate way to {@link #drawPath(List, Paint)}, which uses a {@link PathView}.
     *
     * @param path  {@code float[]} of coordinates, typically projected values
     * @param paint The Paint instance that defines the style of the drawn path.
     */
    public void drawPathQuickly(float[] path, Paint paint) {
        mPathView.addPath(path, paint);
    }

    @Override
    public void onScaleChanged(float scale, float previous) {
        super.onScaleChanged(scale, previous);
        mPathView.setScale(scale);
    }
}
