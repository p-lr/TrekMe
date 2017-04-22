package com.peterlaurence.trekadvisor.menu.tools;

import android.view.MotionEvent;
import android.view.View;

import com.qozix.tileview.TileView;
import com.qozix.tileview.geom.CoordinateTranslater;

/**
 * A touch listener that enables touch-moves of a marker on a {@link TileView}. <br>
 * Example of usage :
 * <pre>{@code
 * MarkerMoveCallback callback = new ClassImplementsMarkerMoveCallback();
 * MarkerTouchMoveListener markerTouchListener = new MarkerTouchMoveListener(tileView, callback);
 * View marker = new CustomMarker(context);
 * marker.setOnTouchListener(markerTouchListener);
 * }</pre>
 *
 * @author peterLaurence
 */
public class MarkerTouchMoveListener implements View.OnTouchListener {
    private final TileView mTileView;
    private float deltaX;
    private float deltaY;
    private MarkerMoveCallback mMarkerMoveCallback;

    private double mLeftBound;
    private double mRightBound;
    private double mTopBound;
    private double mBottomBound;

    public interface MarkerMoveCallback {
        void moveMarker(TileView tileView, View view, double x, double y);
    }

    public MarkerTouchMoveListener(TileView tileView, MarkerMoveCallback markerMoveCallback) {
        mTileView = tileView;
        mMarkerMoveCallback = markerMoveCallback;

        CoordinateTranslater coordinateTranslater = tileView.getCoordinateTranslater();
        mLeftBound = coordinateTranslater.getLeft();
        mRightBound = coordinateTranslater.getRight();
        mTopBound = coordinateTranslater.getTop();
        mBottomBound = coordinateTranslater.getBottom();
    }

    @Override
    public boolean onTouch(final View view, MotionEvent event) {
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                deltaX = event.getX() - view.getWidth() / 2;
                deltaY = event.getY() - view.getHeight() / 2;
                break;

            case MotionEvent.ACTION_MOVE:
                double X = getRelativeX(view.getX() + event.getX() - deltaX);
                double Y = getRelativeY(view.getY() + event.getY() - deltaY);
                mMarkerMoveCallback.moveMarker(mTileView, view, getConstrainedX(X), getConstrainedY(Y));
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

    private double getConstrainedX(double x) {
        if (mLeftBound < mRightBound) {
            if (x <= mLeftBound) {
                return mLeftBound;
            } else if (x >= mRightBound) {
                return mRightBound;
            }
        } else {
            if (x >= mLeftBound) {
                return mLeftBound;
            } else if (x <= mRightBound) {
                return mRightBound;
            }
        }
        return x;
    }

    private double getConstrainedY(double y) {
        if (mBottomBound < mTopBound) {
            if (y <= mBottomBound) {
                return mBottomBound;
            } else if (y >= mTopBound) {
                return mTopBound;
            }
        } else {
            if (y >= mBottomBound) {
                return mBottomBound;
            } else if (y <= mTopBound) {
                return mTopBound;
            }
        }
        return y;
    }
}
