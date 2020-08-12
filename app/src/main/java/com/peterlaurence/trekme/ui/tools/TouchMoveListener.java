package com.peterlaurence.trekme.ui.tools;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.peterlaurence.mapview.MapView;
import com.peterlaurence.mapview.ReferentialData;
import com.peterlaurence.mapview.ReferentialOwner;
import com.peterlaurence.mapview.api.LayoutApiKt;
import com.peterlaurence.mapview.core.CoordinateTranslater;

import org.jetbrains.annotations.NotNull;

/**
 * A touch listener that enables touch-moves of a view (also called marker) on a {@link MapView}. <br>
 * The logic of moving the marker is delegated to the provided {@link MarkerMoveAgent}. <br>
 * It can also react to single-tap event. To be notified, provide a {@link ClickCallback} to
 * the overloaded constructor.
 *
 * <p>
 * Example of usage :
 * <pre>{@code
 * MarkerMoveAgent agent = new ClassImplementsMoveAgent();
 * TouchMoveListener markerTouchListener = new TouchMoveListener(mapView, agent);
 * View marker = new CustomMarker(context);
 * marker.setOnTouchListener(markerTouchListener);
 * mapView.addMarker(marker, ...);
 * }</pre>
 * </p>
 *
 * @author peterLaurence
 */
public class TouchMoveListener extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener, ReferentialOwner {
    private final MapView mMapView;
    private final CoordinateTranslater mCoordinateTranslater;
    private GestureDetector mGestureDetector;
    private float deltaX;
    private float deltaY;
    private MarkerMoveAgent mMarkerMarkerMoveAgent;
    private ClickCallback mMarkerClickCallback;

    private ReferentialData referentialData;

    @NotNull
    @Override
    public ReferentialData getReferentialData() {
        return referentialData;
    }

    @Override
    public void setReferentialData(@NotNull ReferentialData referentialData) {
        this.referentialData = referentialData;
    }

    public TouchMoveListener(MapView mapView, MarkerMoveAgent markerMarkerMoveAgent) {
        this(mapView, markerMarkerMoveAgent, null);
    }

    public TouchMoveListener(MapView mapView, MarkerMoveAgent markerMarkerMoveAgent,
                             ClickCallback markerClickCallback) {
        mMapView = mapView;
        mGestureDetector = new GestureDetector(mapView.getContext(), this);
        mMarkerMarkerMoveAgent = markerMarkerMoveAgent;
        mMarkerClickCallback = markerClickCallback;

        mCoordinateTranslater = mapView.getCoordinateTranslater();
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
                float dX, dY;
                if (rd != null && rd.getRotationEnabled()) {
                    dX = (float) ((event.getX() - deltaX) * Math.cos(angle) - (event.getY() - deltaY) * Math.sin(angle));
                    dY = (float) ((event.getX() - deltaX) * Math.sin(angle) + (event.getY() - deltaY) * Math.cos(angle));
                } else {
                    dX = event.getX() - deltaX;
                    dY = event.getY() - deltaY;
                }
                double X, Y;
                if (rd != null && rd.getRotationEnabled()) {
                    double Xorig = mCoordinateTranslater.reverseRotationX(rd, view.getX() + (view.getWidth() >> 1), view.getY() + (view.getHeight() >> 1));
                    double Yorig = mCoordinateTranslater.reverseRotationY(rd, view.getX() + (view.getWidth() >> 1), view.getY() + (view.getHeight() >> 1));
                    X = getRelativeX((float) Xorig + dX);
                    Y = getRelativeY((float) Yorig + dY);
                } else {
                    X = getRelativeX(view.getX() + dX + (view.getWidth() >> 1));
                    Y = getRelativeY(view.getY() + dY + (view.getHeight() >> 1));
                }
                mMarkerMarkerMoveAgent.onMarkerMove(mMapView, view, getConstrainedX(X), getConstrainedY(Y));
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
        return mCoordinateTranslater.translateAndScaleAbsoluteToRelativeX((int) x, mMapView.getScale());
    }

    private double getRelativeY(float y) {
        return mCoordinateTranslater.translateAndScaleAbsoluteToRelativeY((int) y, mMapView.getScale());
    }

    private double getConstrainedX(double x) {
        return LayoutApiKt.getConstrainedX(mMapView, x);
    }

    private double getConstrainedY(double y) {
        return LayoutApiKt.getConstrainedY(mMapView, y);
    }

    /**
     * A {@link MarkerMoveAgent} is given the "relative coordinates" of the view added to the
     * {@link MapView}.
     * Most of the time, the callee sets the given coordinates of the view on the {@link MapView}.
     */
    public interface MarkerMoveAgent {
        void onMarkerMove(MapView mapView, View view, double x, double y);
    }

    public interface ClickCallback {
        void onMarkerClick();
    }
}
