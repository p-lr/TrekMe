package com.peterlaurence.trekadvisor.menu.mapview;

import android.content.Context;
import android.view.View;

import com.peterlaurence.trekadvisor.menu.mapview.components.MarkerCallout;
import com.peterlaurence.trekadvisor.menu.mapview.components.MarkerGrab;
import com.peterlaurence.trekadvisor.menu.mapview.components.MovableMarker;
import com.peterlaurence.trekadvisor.menu.tools.MarkerTouchMoveListener;
import com.qozix.tileview.TileView;
import com.qozix.tileview.geom.CoordinateTranslater;

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

        movableMarker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                movableMarker.morph();
                MarkerCallout markerCallout = new MarkerCallout(context);
                mTileView.addCallout(markerCallout, relativeX, relativeY, -0.5f, -1.2f);
                markerCallout.transitionIn();
            }
        });

        mTileView.addMarker(movableMarker, relativeX, relativeY, -0.5f, -0.5f);

        /* Easily move the marker */
        attachMarkerGrab(movableMarker, relativeX, relativeY);
    }

    /**
     * A {@link MarkerGrab} is used along with a {@link MarkerTouchMoveListener} to reflect its
     * displacement to the marker passed as argument.
     */
    private void attachMarkerGrab(final MovableMarker movableMarker, double relativeX, double relativeY) {
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
    }
}
