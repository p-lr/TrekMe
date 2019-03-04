package com.peterlaurence.trekme.ui.tools;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.qozix.tileview.TileView;
import com.qozix.tileview.geom.CoordinateTranslater;

/**
 * A touch listener that enables touch-moves of a view (also called marker) on a {@link TileView}. <br>
 * Example of usage :
 * <pre>{@code
 * MoveCallback callback = new ClassImplementsMoveCallback();
 * TouchMoveListener markerTouchListener = new TouchMoveListener(tileView, callback);
 * View marker = new CustomMarker(context);
 * marker.setOnTouchListener(markerTouchListener);
 * tileView.addMarker(marker, ...);
 * }</pre>
 * <p>
 * It can also react to single-tap event. To be notified, provide a {@link ClickCallback} to
 * the overloaded constructor.
 *
 * @author peterLaurence
 */
public class TouchMoveListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {
    private final TileView mTileView;
    private GestureDetector mGestureDetector;
    private float deltaX;
    private float deltaY;
    private MoveCallback mMarkerMoveCallback;
    private ClickCallback mMarkerClickCallback;

    private double mLeftBound;
    private double mRightBound;
    private double mTopBound;
    private double mBottomBound;

    public TouchMoveListener(TileView tileView, MoveCallback markerMoveCallback) {
        this(tileView, markerMoveCallback, null);
    }

    public TouchMoveListener(TileView tileView, MoveCallback markerMoveCallback,
                             ClickCallback markerClickCallback) {
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

    /**
     * A callback that gives the "relative coordinates" of the view added to the TileView.
     * Most of the time, the callee sets the given coordinates of the view on the TileView.
     */
    public interface MoveCallback {
        void onMarkerMove(TileView tileView, View view, double x, double y);
    }

    public interface ClickCallback {
        void onMarkerClick();
    }
}
