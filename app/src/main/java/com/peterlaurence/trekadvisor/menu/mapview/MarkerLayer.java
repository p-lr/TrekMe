package com.peterlaurence.trekadvisor.menu.mapview;

import android.content.Context;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.Drawable;
import android.view.View;

import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.map.gson.MarkerGson;
import com.peterlaurence.trekadvisor.core.map.maploader.MapLoader;
import com.peterlaurence.trekadvisor.menu.mapview.components.MarkerCallout;
import com.peterlaurence.trekadvisor.menu.mapview.components.MarkerGrab;
import com.peterlaurence.trekadvisor.menu.mapview.components.MovableMarker;
import com.peterlaurence.trekadvisor.menu.tools.MarkerTouchMoveListener;
import com.qozix.tileview.TileView;
import com.qozix.tileview.geom.CoordinateTranslater;
import com.qozix.tileview.markers.MarkerLayout;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * All {@link MovableMarker} and {@link MarkerCallout} are managed here. <br>
 * This object is intended to work along with a {@link MapViewFragment}.
 *
 * @author peterLaurence on 09/04/17.
 */
class MarkerLayer implements MapLoader.MapMarkerUpdateListener {
    private MapViewFragment mMapViewFragment;
    private TileView mTileView;
    private Map mMap;
    List<MarkerGson.Marker> mMarkers;
    private MarkerGson.Marker mCurrentMarker;


    MarkerLayer(MapViewFragment mapViewFragment) {
        mMapViewFragment = mapViewFragment;
        MapLoader.getInstance().addMapMarkerUpdateListener(this);
    }

    MarkerGson.Marker getCurrentMarker() {
        return mCurrentMarker;
    }

    @Override
    public void onMapMarkerUpdate() {
        drawMarkers();
    }

    /**
     * A {@link MarkerGrab} is used along with a {@link MarkerTouchMoveListener} to reflect its
     * displacement to the marker passed as argument.
     */
    private static MarkerGrab attachMarkerGrab(final MovableMarker movableMarker, TileView tileView, Context context) {
        /* Add a view as background, to move easily the marker */
        MarkerTouchMoveListener.MarkerMoveCallback markerMoveCallback = new MarkerTouchMoveListener.MarkerMoveCallback() {
            @Override
            public void moveMarker(TileView tileView, View view, double x, double y) {
                tileView.moveMarker(view, x, y);
                tileView.moveMarker(movableMarker, x, y);
                movableMarker.setRelativeX(x);
                movableMarker.setRelativeY(y);
            }
        };

        MarkerGrab markerGrab = new MarkerGrab(context);
        markerGrab.setOnTouchListener(new MarkerTouchMoveListener(tileView, markerMoveCallback));
        tileView.addMarker(markerGrab, movableMarker.getRelativeX(), movableMarker.getRelativeY(), -0.5f, -0.5f);
        markerGrab.morphIn();

        return markerGrab;
    }

    void setTileView(TileView tileView) {
        mTileView = tileView;

        mTileView.setMarkerTapListener(new MarkerLayout.MarkerTapListener() {
            @Override
            public void onMarkerTap(View view, int x, int y) {
                if (view instanceof MovableMarker) {
                    MovableMarker movableMarker = (MovableMarker) view;

                    /* Prepare the callout */
                    MarkerCallout markerCallout = new MarkerCallout(mMapViewFragment.getContext());
                    markerCallout.setMoveAction(new MorphMarkerRunnable(movableMarker, markerCallout, mTileView, mMapViewFragment.getContext()));
                    markerCallout.setEditAction(new EditMarkerRunnable(movableMarker, markerCallout, mTileView,
                            (MapViewFragment.RequestManageMarkerListener) mMapViewFragment.getActivity()));

                    mTileView.addCallout(markerCallout, movableMarker.getRelativeX(), movableMarker.getRelativeY(), -0.5f, -1.2f);
                    markerCallout.transitionIn();
                }
            }
        });
    }

    void setMap(Map map) {
        mMap = map;

        /* Update the ui accordingly */
        init();
    }

    /**
     * Triggers the fetch of the map's markers and their drawing on the {@link TileView}. If this is
     * the first time this method is called for this map, the markers aren't defined and the
     * {@link MapLoader} will get them in an asynctask. Otherwise, we can draw them immediately.<br>
     * This must be called when the {@link MapViewFragment} is ready to update its UI.
     */
    private void init() {
        if (mMap.areMarkersDefined()) {
            drawMarkers();
        } else {
            MapLoader.getInstance().getMarkersForMap(mMap);
        }
    }

    private void drawMarkers() {
        mMarkers = mMap.getMarkers();

    }

    /**
     * Add a {@link MovableMarker} to the center of the {@link TileView}.
     */
    void addNewMarker() {
        /* Calculate the relative coordinates of the center of the screen */
        int x = mTileView.getScrollX() + mTileView.getWidth() / 2 - mTileView.getOffsetX();
        int y = mTileView.getScrollY() + mTileView.getHeight() / 2 - mTileView.getOffsetY();
        CoordinateTranslater coordinateTranslater = mTileView.getCoordinateTranslater();
        final double relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mTileView.getScale());
        final double relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mTileView.getScale());

        final MovableMarker movableMarker;
        Context context = mMapViewFragment.getContext();

        /* Create a new marker and add it to the map */
        MarkerGson.Marker newMarker = new MarkerGson.Marker();
        if (mMap != null) {
            mMap.addMarker(newMarker);
        }

        /* Create the corresponding view */
        movableMarker = new MovableMarker(context, newMarker);

        /* Easily move the marker */
        movableMarker.setRelativeX(relativeX);
        movableMarker.setRelativeY(relativeY);
        MarkerGrab markerGrab = attachMarkerGrab(movableMarker, mTileView, mMapViewFragment.getContext());

        movableMarker.setOnClickListener(new MovableMarkerClickListener(movableMarker, markerGrab, mTileView));

        mTileView.addMarker(movableMarker, relativeX, relativeY, -0.5f, -0.5f);
    }

    private static class MovableMarkerClickListener implements View.OnClickListener {
        private WeakReference<MovableMarker> mMovableMarkerWeakReference;
        private WeakReference<MarkerGrab> mMarkerGrabWeakReference;
        private TileView mTileView;

        MovableMarkerClickListener(MovableMarker movableMarker, MarkerGrab markerGrab, TileView tileView) {
            mMovableMarkerWeakReference = new WeakReference<>(movableMarker);
            mMarkerGrabWeakReference = new WeakReference<>(markerGrab);
            mTileView = tileView;
        }

        @Override
        public void onClick(View v) {
            MovableMarker movableMarker = mMovableMarkerWeakReference.get();
            if (movableMarker != null) {
                movableMarker.morphToStaticForm();

                /* After the morph, the marker should not consume touch events */
                movableMarker.setClickable(false);


                final MarkerGrab markerGrab = mMarkerGrabWeakReference.get();
                markerGrab.morphOut(new Animatable2.AnimationCallback() {
                    @Override
                    public void onAnimationEnd(Drawable drawable) {
                        super.onAnimationEnd(drawable);
                        if (markerGrab != null) {
                            mTileView.removeMarker(markerGrab);
                        }
                    }
                });
            }
        }
    }

    /**
     * This {@link Runnable} is called when an external component requests a {@link MovableMarker} to
     * morph into the dynamic shape. <br>Here, this component is a {@link MarkerCallout}.
     */
    private static class MorphMarkerRunnable implements Runnable {
        private WeakReference<MovableMarker> mMovableMarkerWeakReference;
        private WeakReference<MarkerCallout> mMarkerCalloutWeakReference;
        private TileView mTileView;
        private Context mContext;

        MorphMarkerRunnable(MovableMarker movableMarker, MarkerCallout markerCallout, TileView tileView, Context context) {
            mMovableMarkerWeakReference = new WeakReference<>(movableMarker);
            mMarkerCalloutWeakReference = new WeakReference<>(markerCallout);
            mTileView = tileView;
            mContext = context;
        }

        @Override
        public void run() {
            MovableMarker movableMarker = mMovableMarkerWeakReference.get();

            if (movableMarker != null) {
                movableMarker.morphToDynamicForm();

                /* Easily move the marker */
                MarkerGrab markerGrab = attachMarkerGrab(movableMarker, mTileView, mContext);
                movableMarker.setOnClickListener(new MovableMarkerClickListener(movableMarker, markerGrab, mTileView));

                /* Use a trick to bring the marker to the foreground */
                mTileView.removeMarker(movableMarker);
                mTileView.addMarker(movableMarker, movableMarker.getRelativeX(), movableMarker.getRelativeY(), -0.5f, -0.5f);
            }

            /* Remove the callout */
            MarkerCallout markerCallout = mMarkerCalloutWeakReference.get();
            if (markerCallout != null) {
                mTileView.removeCallout(markerCallout);
            }
        }
    }

    /**
     * This {@link Runnable} is called when an external component requests a {@link MovableMarker} to
     * be edited. <br>Here, this component is a {@link MarkerCallout}.
     */
    private static class EditMarkerRunnable implements Runnable {
        private WeakReference<MovableMarker> mMovableMarkerWeakReference;
        private WeakReference<MarkerCallout> mMarkerCalloutWeakReference;
        private TileView mTileView;
        private WeakReference<MapViewFragment.RequestManageMarkerListener> mListenerWeakRef;

        EditMarkerRunnable(MovableMarker movableMarker, MarkerCallout markerCallout, TileView tileView,
                           MapViewFragment.RequestManageMarkerListener listener) {
            mMovableMarkerWeakReference = new WeakReference<>(movableMarker);
            mMarkerCalloutWeakReference = new WeakReference<>(markerCallout);
            mTileView = tileView;
            mListenerWeakRef = new WeakReference<>(listener);
        }

        @Override
        public void run() {
            MovableMarker movableMarker = mMovableMarkerWeakReference.get();

            if (movableMarker != null) {
                if (mListenerWeakRef != null) {
                    MapViewFragment.RequestManageMarkerListener listener = mListenerWeakRef.get();
                    if (listener != null) {
                        // TODO : implement this
                        listener.onRequestManageMarker();
                    }
                }
            }

            /* Remove the callout */
            MarkerCallout markerCallout = mMarkerCalloutWeakReference.get();
            if (markerCallout != null) {
                mTileView.removeCallout(markerCallout);
            }
        }
    }
}
