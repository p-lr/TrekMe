package com.peterlaurence.trekme.ui.tools;

import android.view.GestureDetector;
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
 * <p>
 * It can also react to single-tap event. To be notified, provide a {@link MarkerClickCallback} to
 * the overloaded constructor.
 *
 * @author peterLaurence
 */
public class MarkerTouchMoveListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {
    private final TileView mTileView;
    private GestureDetector mGestureDetector;
    private float deltaX;
    private float deltaY;
    private MarkerMoveCallback mMarkerMoveCallback;
    private MarkerClickCallback mMarkerClickCallback;

    private double mLeftBound;
    private double mRightBound;
    private double mTopBound;
    private double mBottomBound;

    public MarkerTouchMoveListener(TileView tileView, MarkerMoveCallback markerMoveCallback) {
        this(tileView, markerMoveCallback, null);
    }

    public MarkerTouchMoveListener(TileView tileView, MarkerMoveCallback markerMoveCallback,
                                   MarkerClickCallback markerClickCallback) {
        mTileView = tileView;
        mGestureDetector = new GestureDetector(tileView.getContext(), this);
        mMarkerMoveCallback = markerMoveCallback;
        mMarkerClickCallback = markerClickCallback;

        CoordinateTranslater coordinateTranslater = tileView.getCoordinateTranslater();
        mLeftBound = coordinateTranslater.getLeft();
        mRightBound = coordinateTranslater.getRight();
        mTopBound = coordinateTranslater.getTop();
        mBottomBound = coordinateTranslater.getBottom();
    }

    @Override
    public boolean onTouch(final View view, MotionEvent event) {
        if (mMarkerClickCallback != null && mGestureDetector.onTouchEvent(event)) {
            return true;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                deltaX = event.getX() - view.getWidth() / 2;
                deltaY = event.getY() - view.getHeight() / 2;
                break;

            case MotionEvent.ACTION_MOVE:
                double X = getRelativeX(view.getX() + event.getX() - deltaX);
                double Y = getRelativeY(view.getY() + event.getY() - deltaY);
                mMarkerMoveCallback.onMarkerMove(mTileView, view, getConstrainedX(X), getConstrainedY(Y));
                break;

            default:
                return false;
        }
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (mMarkerClickCallback != null) {
            mMarkerClickCallback.onMarkerClick();
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

    public interface MarkerMoveCallback {
        void onMarkerMove(TileView tileView, View view, double x, double y);
    }

    public interface MarkerClickCallback {
        void onMarkerClick();
    }
}
