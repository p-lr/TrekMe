package com.peterlaurence.trekadvisor.menu.mapview;

import android.content.Context;
import android.view.View;

import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.menu.mapview.components.DistanceMarker;
import com.peterlaurence.trekadvisor.menu.mapview.components.DistanceView;
import com.qozix.tileview.TileView;
import com.qozix.tileview.geom.CoordinateTranslater;

/**
 * Shows two {@link DistanceMarker} and a {@link DistanceView}.
 *
 * @author peterLaurence on 17/06/17.
 */
public class DistanceLayer {
    private View mParentView;
    private Context mContext;
    private DistanceMarker mDistanceMarkerFirst;
    private DistanceMarker mDistanceMarkerSecond;

    private Map mMap;
    private TileView mTileView;

    DistanceLayer(View parentView, Context context, TileView tileView) {
        mParentView = parentView;
        mContext = context;

        mTileView = tileView;

        mDistanceMarkerFirst = new DistanceMarker(mContext);
        mDistanceMarkerSecond = new DistanceMarker(mContext);

        drawDistanceMarkers();
    }

    /**
     * Hide the two {@link DistanceMarker} and the {@link DistanceView}.
     */
    public void hide() {
        mTileView.removeMarker(mDistanceMarkerFirst);
        mTileView.removeMarker(mDistanceMarkerSecond);
        mContext = null;
        mParentView = null;
    }

    private void drawDistanceMarkers() {
        /* Calculate the relative coordinates of the first marker */
        int x = mTileView.getScrollX() + mTileView.getWidth() / 3 - mTileView.getOffsetX();
        int y = mTileView.getScrollY() + mTileView.getHeight() / 2 - mTileView.getOffsetY();
        CoordinateTranslater coordinateTranslater = mTileView.getCoordinateTranslater();
        double relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mTileView.getScale());
        double relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mTileView.getScale());

        mDistanceMarkerFirst.setRelativeX(relativeX);
        mDistanceMarkerFirst.setRelativeY(relativeY);

        /* Calculate the relative coordinates of the first marker */
        x = mTileView.getScrollX() + mTileView.getWidth() * 2 / 3 - mTileView.getOffsetX();
        relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mTileView.getScale());
        relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mTileView.getScale());

        mDistanceMarkerSecond.setRelativeX(relativeX);
        mDistanceMarkerSecond.setRelativeY(relativeY);
    }
}
