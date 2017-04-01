package com.peterlaurence.trekadvisor.menu.tools;

import android.view.MotionEvent;
import android.view.View;

import com.qozix.tileview.TileView;

/**
 * A touch listener that enables touch-moves of a marker on a {@link TileView}. <br>
 * Example of usage :
 * <pre>{@code
 * MarkerMoveCallback callback = new ClassImplementsMarkerMoveCallback();
 * MarkerTouchMoveListener markerTouchListener = new MarkerTouchMoveListener(tileView, callback);
 * mPositionMarker = new PositionMarker(context);
 * mPositionMarker.setOnTouchListener(markerTouchListener);
 * }</pre>
 *
 * @author peterLaurence
 */
public class MarkerTouchMoveListener implements View.OnTouchListener {
    private final TileView mTileView;
    private double deltaX;
    private double deltaY;
    private MarkerMoveCallback mMarkerMoveCallback;

    public interface MarkerMoveCallback {
        void moveMarker(TileView tileView, View view, double x, double y);
    }

    public MarkerTouchMoveListener(TileView tileView, MarkerMoveCallback markerMoveCallback) {
        mTileView = tileView;
        mMarkerMoveCallback = markerMoveCallback;
    }

    @Override
    public boolean onTouch(final View view, MotionEvent event) {
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                deltaX = getRelativeX(event.getX() - view.getWidth() / 2);
                deltaY = getRelativeY(event.getY() - view.getHeight() / 2);
                break;

            case MotionEvent.ACTION_MOVE:
                double X = getRelativeX(view.getX() + event.getX());
                double Y = getRelativeY(view.getY() + event.getY());
                mMarkerMoveCallback.moveMarker(mTileView, view, X - deltaX, Y - deltaY);
                break;

            default:
                return false;
        }
        return true;
    }

    private double getRelativeX(float x) {
        return mTileView.getCoordinateTranslater().translateAndScaleAbsoluteToRelativeX(x, mTileView.getScale());
    }

    private double getRelativeY(float y) {
        return mTileView.getCoordinateTranslater().translateAndScaleAbsoluteToRelativeY(y, mTileView.getScale());
    }
}
