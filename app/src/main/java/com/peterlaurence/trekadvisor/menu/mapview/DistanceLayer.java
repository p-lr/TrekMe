package com.peterlaurence.trekadvisor.menu.mapview;

import android.content.Context;
import android.view.View;

import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.menu.mapview.components.DistanceMarker;
import com.peterlaurence.trekadvisor.menu.mapview.components.DistanceView;
import com.peterlaurence.trekadvisor.menu.tools.MarkerTouchMoveListener;
import com.qozix.tileview.TileView;
import com.qozix.tileview.geom.CoordinateTranslater;

/**
 * Shows two {@link DistanceMarker} and a {@link DistanceView}.
 *
 * @author peterLaurence on 17/06/17.
 */
public class DistanceLayer {
    private Context mContext;
    private DistanceMarker mDistanceMarkerFirst;
    private DistanceMarker mDistanceMarkerSecond;
    private boolean mVisible;

    private Map mMap;
    private TileView mTileView;

    private double mFirstMarkerRelativeX;
    private double mFirstMarkerRelativeY;
    private double mSecondMarkerRelativeX;
    private double mSecondMarkerRelativeY;

    DistanceLayer(Context context) {
        mContext = context;
        mVisible = false;
    }

    public void init(TileView tileView) {
        mTileView = tileView;
    }

    /**
     * Shows the two {@link DistanceMarker} and the {@link DistanceView}.<br>
     * {@link #init(TileView)} must have been called before.
     */
    public void show() {
        /* Setup the first marker */
        mDistanceMarkerFirst = new DistanceMarker(mContext);
        MarkerTouchMoveListener.MarkerMoveCallback firstMarkerMoveCallback = new MarkerTouchMoveListener.MarkerMoveCallback() {
            @Override
            public void onMarkerMove(TileView tileView, View view, double x, double y) {
                mFirstMarkerRelativeX = x;
                mFirstMarkerRelativeY = y;
                tileView.moveMarker(mDistanceMarkerFirst, x, y);
            }
        };
        MarkerTouchMoveListener firstMarkerTouchMoveListener = new MarkerTouchMoveListener(mTileView, firstMarkerMoveCallback);
        mDistanceMarkerFirst.setOnTouchListener(firstMarkerTouchMoveListener);

        /* Setup the second marker*/
        mDistanceMarkerSecond = new DistanceMarker(mContext);
        MarkerTouchMoveListener.MarkerMoveCallback secondMarkerMoveCallback = new MarkerTouchMoveListener.MarkerMoveCallback() {
            @Override
            public void onMarkerMove(TileView tileView, View view, double x, double y) {
                mSecondMarkerRelativeX = x;
                mSecondMarkerRelativeY = y;
                tileView.moveMarker(mDistanceMarkerSecond, x, y);
            }
        };
        MarkerTouchMoveListener secondMarkerTouchMoveListener = new MarkerTouchMoveListener(mTileView, secondMarkerMoveCallback);
        mDistanceMarkerSecond.setOnTouchListener(secondMarkerTouchMoveListener);

        /* Set their positions */
        initDistanceMarkers();

        /* ..and add them to the TileView */
        mTileView.addMarker(mDistanceMarkerFirst, mFirstMarkerRelativeX, mFirstMarkerRelativeY,
                -0.5f, -0.5f);
        mTileView.addMarker(mDistanceMarkerSecond, mSecondMarkerRelativeX, mSecondMarkerRelativeY,
                -0.5f, -0.5f);
        mVisible = true;
    }

    /**
     * Hide the two {@link DistanceMarker} and the {@link DistanceView}.
     */
    public void hide() {
        mTileView.removeMarker(mDistanceMarkerFirst);
        mTileView.removeMarker(mDistanceMarkerSecond);

        mDistanceMarkerFirst = null;
        mDistanceMarkerSecond = null;
        mVisible = false;
    }

    public boolean isVisible() {
        return mVisible;
    }

    private void initDistanceMarkers() {
        /* Calculate the relative coordinates of the first marker */
        int x = mTileView.getScrollX() + mTileView.getWidth() / 3 - mTileView.getOffsetX();
        int y = mTileView.getScrollY() + mTileView.getHeight() / 3 - mTileView.getOffsetY();
        CoordinateTranslater coordinateTranslater = mTileView.getCoordinateTranslater();
        double relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mTileView.getScale());
        double relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mTileView.getScale());

        mFirstMarkerRelativeX = relativeX;
        mFirstMarkerRelativeY = relativeY;

        /* Calculate the relative coordinates of the second marker */
        x = mTileView.getScrollX() + mTileView.getWidth() * 2 / 3 - mTileView.getOffsetX();
        y = mTileView.getScrollY() + mTileView.getHeight() * 2 / 3 - mTileView.getOffsetY();
        relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mTileView.getScale());
        relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mTileView.getScale());

        mSecondMarkerRelativeX = relativeX;
        mSecondMarkerRelativeY = relativeY;
    }
}
