package com.peterlaurence.trekadvisor.menu.mapview;

import android.content.Context;
import android.view.MotionEvent;

import com.peterlaurence.trekadvisor.core.map.gson.RouteGson;
import com.peterlaurence.trekadvisor.menu.mapview.components.PathView;
import com.qozix.tileview.TileView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
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
    private List<ScaleChangeListener> mScaleChangeListeners;
    private PathView mPathView;

    public TileViewExtended(Context context) {
        super(context);

        mScaleChangeListeners = new ArrayList<>();
        mPathView = new PathView(context);
        addView(mPathView, getChildCount() - 1);
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

    public void setSingleTapListener(SingleTapStaticListener listener) {
        mSingleTapListenerWeakReference = new WeakReference<>(listener);
    }

    public void setScrollListener(ScrollListener listener) {
        mScrollListenerWeakReference = new WeakReference<>(listener);
    }

    public PathView getPathView() {
        return mPathView;
    }

    /**
     * Updates the {@link PathView}. It expects that each {@link RouteGson.Route} has a data object
     * of type {@link PathView.DrawablePath}.
     */
    public void drawRoutes(List<RouteGson.Route> routeList) {
        mPathView.updateRoutes(routeList);
    }

    @Override
    public void onScaleChanged(float scale, float previous) {
        super.onScaleChanged(scale, previous);

        for (ScaleChangeListener listener : mScaleChangeListeners) {
            listener.onScaleChanged(scale);
        }

        // TODO : set the PathView as a scale listener
        mPathView.setScale(scale);
    }

    public void addScaleChangeListener(ScaleChangeListener listener) {
        mScaleChangeListeners.add(listener);
    }

    public void removeScaleChangeLisetner(ScaleChangeListener listener) {
        mScaleChangeListeners.remove(listener);
    }

    public interface ScaleChangeListener {
        void onScaleChanged(float scale);
    }

    /**
     * On a tap up, if the {@link TileView} was not scrolling when the touch down occurred, we
     * consider that event eligible for a SingleTapStatic event.
     */
    public interface SingleTapStaticListener {
        void onSingleTapStatic();
    }

    public interface ScrollListener {
        void onScroll();
    }
}
