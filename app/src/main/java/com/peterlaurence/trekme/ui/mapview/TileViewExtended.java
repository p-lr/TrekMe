package com.peterlaurence.trekme.ui.mapview;

import android.content.Context;
import androidx.annotation.Nullable;
import android.view.MotionEvent;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.gson.RouteGson;
import com.peterlaurence.trekme.ui.mapview.components.PathView;
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
    private PathView mLiveRouteView;

    public TileViewExtended(Context context) {
        super(context);

        mScaleChangeListeners = new ArrayList<>();
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

    @Nullable
    public PathView getPathView() {
        return mPathView;
    }

    /**
     * Updates the {@link PathView}. It expects that each {@link RouteGson.Route} has a data object
     * of type {@link PathView.DrawablePath}.
     */
    public void drawRoutes(List<RouteGson.Route> routeList) {
        if (mPathView == null) {
            createPathView();
        }
        mPathView.updateRoutes(routeList);
    }

    /**
     * Updates the {@link PathView} that represents the live route.
     * @param routeList A list of size 1 of {@link RouteGson.Route}.
     */
    public void drawLiveRoute(List<RouteGson.Route> routeList) {
        if (mLiveRouteView == null) {
            createLiveRouteView();
        }

        mLiveRouteView.updateRoutes(routeList);
    }

    @Override
    public void onScaleChanged(float scale, float previous) {
        super.onScaleChanged(scale, previous);

        for (ScaleChangeListener listener : mScaleChangeListeners) {
            listener.onScaleChanged(scale);
        }
    }

    public void addScaleChangeListener(ScaleChangeListener listener) {
        mScaleChangeListeners.add(listener);
    }

    public void removeScaleChangeListener(ScaleChangeListener listener) {
        mScaleChangeListeners.remove(listener);
    }

    public interface ScaleChangeListener {
        void onScaleChanged(float scale);
    }

    private void createPathView() {
        mPathView = new PathView(getContext());
        addView(mPathView, 1);
        addScaleChangeListener(scale -> mPathView.setScale(scale));
        mPathView.setScale(getScale());
    }

    private void createLiveRouteView() {
        mLiveRouteView = new PathView(getContext());
        mLiveRouteView.getDefaultPaint().setColor(getContext().getColor(R.color.colorLiveRoute));
        addView(mLiveRouteView, 1);
        addScaleChangeListener(scale -> mLiveRouteView.setScale(scale));
        mLiveRouteView.setScale(getScale());
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
