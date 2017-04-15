package com.peterlaurence.trekadvisor.menu.mapview;

import android.content.Context;
import android.view.View;

import com.peterlaurence.trekadvisor.menu.mapview.components.MarkerCallout;
import com.peterlaurence.trekadvisor.menu.mapview.components.MarkerGrab;
import com.peterlaurence.trekadvisor.menu.mapview.components.MovableMarker;
import com.peterlaurence.trekadvisor.menu.tools.MarkerTouchMoveListener;
import com.qozix.tileview.TileView;
import com.qozix.tileview.geom.CoordinateTranslater;
import com.qozix.tileview.markers.MarkerLayout;

import java.lang.ref.WeakReference;

/**
 * All {@link MovableMarker} and {@link MarkerCallout} are managed here. <br>
 * This object is intended to work along with a {@link MapViewFragment}.
 *
 * @author peterLaurence on 09/04/17.
 */
class MarkerLayer {
    private MapViewFragment mMapViewFragment;
    private TileView mTileView;


    MarkerLayer(MapViewFragment mapViewFragment) {
        mMapViewFragment = mapViewFragment;
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
                    markerCallout.setMoveAction(new MorphMarkerRunnable(movableMarker, markerCallout, mTileView));

                    CoordinateTranslater coordinateTranslater = mTileView.getCoordinateTranslater();
                    double relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mTileView.getScale());
                    double relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mTileView.getScale());
                    mTileView.addCallout(markerCallout, relativeX, relativeY, -0.5f, -1.2f);
                    markerCallout.transitionIn();
                }
            }
        });
    }

    /**
     * Add a {@link MovableMarker} to the center of the {@link TileView}.
     */
    void addMarker() {
        /* Calculate the relative coordinates of the center of the screen */
        int x = mTileView.getScrollX() + mTileView.getWidth() / 2 - mTileView.getOffsetX();
        int y = mTileView.getScrollY() + mTileView.getHeight() / 2 - mTileView.getOffsetY();
        CoordinateTranslater coordinateTranslater = mTileView.getCoordinateTranslater();
        final double relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mTileView.getScale());
        final double relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mTileView.getScale());

        final MovableMarker movableMarker;
        final Context context = mMapViewFragment.getContext();
        movableMarker = new MovableMarker(context);

        /* Easily move the marker */
        MarkerGrab markerGrab = attachMarkerGrab(movableMarker, relativeX, relativeY);

        movableMarker.setOnClickListener(new MovableMarkerClickListener(movableMarker, markerGrab, mTileView));

        mTileView.addMarker(movableMarker, relativeX, relativeY, -0.5f, -0.5f);
    }

    /**
     * A {@link MarkerGrab} is used along with a {@link MarkerTouchMoveListener} to reflect its
     * displacement to the marker passed as argument.
     */
    private MarkerGrab attachMarkerGrab(final MovableMarker movableMarker, double relativeX, double relativeY) {
        /* Add a view as background, to move easily the marker */
        MarkerTouchMoveListener.MarkerMoveCallback markerMoveCallback = new MarkerTouchMoveListener.MarkerMoveCallback() {
            @Override
            public void moveMarker(TileView tileView, View view, double x, double y) {
                tileView.moveMarker(view, x, y);
                tileView.moveMarker(movableMarker, x, y);
            }
        };

        MarkerGrab markerGrab = new MarkerGrab(mMapViewFragment.getContext());
        markerGrab.setOnTouchListener(new MarkerTouchMoveListener(mTileView, markerMoveCallback));
        mTileView.addMarker(markerGrab, relativeX, relativeY, -0.5f, -0.5f);

        return markerGrab;
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

                MarkerGrab markerGrab = mMarkerGrabWeakReference.get();
                if (markerGrab != null) {
                    mTileView.removeMarker(markerGrab);
                }
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

        MorphMarkerRunnable(MovableMarker movableMarker, MarkerCallout markerCallout, TileView tileView) {
            mMovableMarkerWeakReference = new WeakReference<>(movableMarker);
            mMarkerCalloutWeakReference = new WeakReference<>(markerCallout);
            mTileView = tileView;
        }

        @Override
        public void run() {
            MovableMarker movableMarker = mMovableMarkerWeakReference.get();

            if (movableMarker != null) {
                movableMarker.morphToDynamicForm();
            }

            /* Remove the callout */
            MarkerCallout markerCallout = mMarkerCalloutWeakReference.get();
            if (markerCallout != null) {
                mTileView.removeCallout(markerCallout);
            }
        }
    }
}
