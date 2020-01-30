package com.peterlaurence.trekme.ui.tools;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.peterlaurence.mapview.MapView;
import com.peterlaurence.mapview.ReferentialData;
import com.peterlaurence.mapview.core.CoordinateTranslater;

/**
 * A touch listener that enables touch-moves of a view (also called marker) on a {@link MapView}. <br>
 * Example of usage :
 * <pre>{@code
 * MoveCallback callback = new ClassImplementsMoveCallback();
 * TouchMoveListener markerTouchListener = new TouchMoveListener(mapView, callback);
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
    private final MapView mMapView;
    private GestureDetector mGestureDetector;
    private float deltaX;
    private float deltaY;
    private MoveCallback mMarkerMoveCallback;
    private ClickCallback mMarkerClickCallback;

    private double mLeftBound;
    private double mRightBound;
    private double mTopBound;
    private double mBottomBound;

    public ReferentialData referentialData;

    public TouchMoveListener(MapView mapView, MoveCallback markerMoveCallback) {
        this(mapView, markerMoveCallback, null);
    }

    public TouchMoveListener(MapView mapView, MoveCallback markerMoveCallback,
                             ClickCallback markerClickCallback) {
        mMapView = mapView;
        mGestureDetector = new GestureDetector(mapView.getContext(), this);
        mMarkerMoveCallback = markerMoveCallback;
        mMarkerClickCallback = markerClickCallback;

        CoordinateTranslater coordinateTranslater = mapView.getCoordinateTranslater();
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

        ReferentialData rd = referentialData;
        double angle = 0.0;
        if (rd != null) {
            angle = -Math.toRadians(rd.getAngle());
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                deltaX = event.getX();
                deltaY = event.getY();
                break;

            case MotionEvent.ACTION_MOVE:
                float dX;
                float dY;
                if (rd != null && rd.getRotationEnabled()) {
                    dX = (float) ((event.getX() - deltaX) * Math.cos(angle) - (event.getY() - deltaY) * Math.sin(angle));
                    dY = (float) ((event.getX() - deltaX) * Math.sin(angle) + (event.getY() - deltaY) * Math.cos(angle));
                } else {
                    dX = event.getX() - deltaX;
                    dY = event.getY() - deltaY;
                }
                if (rd != null && rd.getRotationEnabled()) {
                    double X = getRelativeX(getXorig(view.getX() + (view.getWidth() >> 1), view.getY() + (view.getHeight() >> 1), angle) + dX);
                    double Y = getRelativeY(getYorig(view.getX() + (view.getWidth() >> 1), view.getY() + (view.getHeight() >> 1), angle) + dY);
                    mMarkerMoveCallback.onMarkerMove(mMapView, view, getConstrainedX(X), getConstrainedY(Y));
                } else {
                    double X = getRelativeX(view.getX() + dX + (view.getWidth() >> 1));
                    double Y = getRelativeY(view.getY() + dY + (view.getHeight() >> 1));
                    mMarkerMoveCallback.onMarkerMove(mMapView, view, getConstrainedX(X), getConstrainedY(Y));
                }
                break;

            default:
                return false;
        }
        return true;
    }

    private float getXorig(float Xr, float Yr, double angle) {
        ReferentialData rd = referentialData;
        double Cx = rd.getCenterX() * mMapView.getCoordinateTranslater().getBaseWidth() * rd.getScale();
        double Cy = rd.getCenterY() * mMapView.getCoordinateTranslater().getBaseHeight() * rd.getScale();
        return (float) (Cx + (Xr - Cx) * Math.cos(angle) - (Yr - Cy) * Math.sin(angle));
    }

    private float getYorig(float Xr, float Yr, double angle) {
        ReferentialData rd = referentialData;
        double Cx = rd.getCenterX() * mMapView.getCoordinateTranslater().getBaseWidth() * rd.getScale();
        double Cy = rd.getCenterY() * mMapView.getCoordinateTranslater().getBaseHeight() * rd.getScale();
        return (float) (Cy + (Xr - Cx) * Math.sin(angle) + (Yr - Cy) * Math.cos(angle));
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (mMarkerClickCallback != null) {
            mMarkerClickCallback.onMarkerClick();
        }
        return true;
    }

    private double getRelativeX(float x) {
        return mMapView.getCoordinateTranslater().translateAndScaleAbsoluteToRelativeX((int) x, mMapView.getScale());
    }

    private double getRelativeY(float y) {
        return mMapView.getCoordinateTranslater().translateAndScaleAbsoluteToRelativeY((int) y, mMapView.getScale());
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
     * A callback that gives the "relative coordinates" of the view added to the MapView.
     * Most of the time, the callee sets the given coordinates of the view on the MapView.
     */
    public interface MoveCallback {
        void onMarkerMove(MapView mapView, View view, double x, double y);
    }

    public interface ClickCallback {
        void onMarkerClick();
    }
}
