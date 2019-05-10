package com.peterlaurence.trekme.ui.mapview;

import android.content.Context;

import androidx.annotation.Nullable;

import com.peterlaurence.trekme.R;
import com.peterlaurence.trekme.core.map.gson.RouteGson;
import com.peterlaurence.trekme.ui.mapview.components.PathView;
import com.qozix.tileview.TileView;

import java.util.ArrayList;
import java.util.List;

/**
 * Specialization of TileView to allow custom custom control of touch events.
 *
 * @author peterLaurence on 28/03/16.
 */
public class TileViewExtended extends TileView {

    private List<ScaleChangeListener> mScaleChangeListeners;
    private PathView mPathView;
    private PathView mLiveRouteView;

    public TileViewExtended(Context context) {
        super(context);

        mScaleChangeListeners = new ArrayList<>();
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
     *
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
}
