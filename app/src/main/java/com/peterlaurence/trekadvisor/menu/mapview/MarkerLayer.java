package com.peterlaurence.trekadvisor.menu.mapview;

import android.content.Context;
import android.view.View;

import com.peterlaurence.trekadvisor.menu.mapview.components.MarkerCallout;
import com.peterlaurence.trekadvisor.menu.mapview.components.MovableMarker;
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

        final MovableMarker movableMaker;
        final Context context = mMapViewFragment.getContext();
        movableMaker = new MovableMarker(context);

        movableMaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                movableMaker.morph();
                MarkerCallout markerCallout = new MarkerCallout(context);
                mTileView.addCallout(markerCallout, relativeX, relativeY, -0.5f, -1.2f);
                markerCallout.transitionIn();
            }
        });

        mTileView.addMarker(movableMaker, relativeX, relativeY, -0.5f, -0.5f);
    }
}
