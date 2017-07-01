package com.peterlaurence.trekadvisor.menu.mapview;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;

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
    HandlerThread mDistanceThread;
    Handler mHandler;
    private Context mContext;
    private DistanceMarker mDistanceMarkerFirst;
    private DistanceMarker mDistanceMarkerSecond;
    private DistanceView mDistanceView;
    private boolean mVisible;
    private DistanceListener mDistanceListener;
    private TileViewExtended mTileView;

    private double mFirstMarkerRelativeX;
    private double mFirstMarkerRelativeY;
    private double mSecondMarkerRelativeX;
    private double mSecondMarkerRelativeY;

    DistanceLayer(Context context, DistanceListener listener) {
        mContext = context;
        mVisible = false;
        mDistanceListener = listener;
    }

    public void init(TileViewExtended tileView) {
        mTileView = tileView;
    }

    /**
     * Shows the two {@link DistanceMarker} and the {@link DistanceView}.<br>
     * {@link #init(TileViewExtended)} must have been called before.
     */
    public void show() {
        /* Create the DistanceView (the line between the two markers) */
        mDistanceView = new DistanceView(mContext, mTileView.getScale());
        mTileView.addScaleChangeListener(mDistanceView);
        mTileView.addView(mDistanceView);

        /* Setup the first marker */
        mDistanceMarkerFirst = new DistanceMarker(mContext);
        MarkerTouchMoveListener.MarkerMoveCallback firstMarkerMoveCallback = new MarkerTouchMoveListener.MarkerMoveCallback() {
            @Override
            public void onMarkerMove(TileView tileView, View view, double x, double y) {
                mFirstMarkerRelativeX = x;
                mFirstMarkerRelativeY = y;
                tileView.moveMarker(mDistanceMarkerFirst, x, y);
                updateDistanceView();
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
                updateDistanceView();
            }
        };
        MarkerTouchMoveListener secondMarkerTouchMoveListener = new MarkerTouchMoveListener(mTileView, secondMarkerMoveCallback);
        mDistanceMarkerSecond.setOnTouchListener(secondMarkerTouchMoveListener);

        /* Set their positions */
        initDistanceMarkers();
        updateDistanceView();

        /* ..and add them to the TileView */
        mTileView.addMarker(mDistanceMarkerFirst, mFirstMarkerRelativeX, mFirstMarkerRelativeY,
                -0.5f, -0.5f);
        mTileView.addMarker(mDistanceMarkerSecond, mSecondMarkerRelativeX, mSecondMarkerRelativeY,
                -0.5f, -0.5f);
        mVisible = true;

        /* Start the thread that will process distance calculations */
        prepareDistanceCalculation();
    }

    /**
     * Hide the two {@link DistanceMarker} and the {@link DistanceView}.
     */
    public void hide() {
        mTileView.removeMarker(mDistanceMarkerFirst);
        mTileView.removeMarker(mDistanceMarkerSecond);
        mTileView.removeView(mDistanceView);
        mTileView.removeScaleChangeLisetner(mDistanceView);

        mDistanceMarkerFirst = null;
        mDistanceMarkerSecond = null;
        mDistanceView = null;
        mVisible = false;

        /* Stop the thread that process distance calculation */
        stopDistanceCalculation();
    }

    public boolean isVisible() {
        return mVisible;
    }

    private void initDistanceMarkers() {
        /* Calculate the relative coordinates of the first marker */
        int x = mTileView.getScrollX() + mTileView.getWidth() * 2 / 3 - mTileView.getOffsetX();
        int y = mTileView.getScrollY() + mTileView.getHeight() / 3 - mTileView.getOffsetY();
        CoordinateTranslater coordinateTranslater = mTileView.getCoordinateTranslater();
        double relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mTileView.getScale());
        double relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mTileView.getScale());

        mFirstMarkerRelativeX = relativeX;
        mFirstMarkerRelativeY = relativeY;

        /* Calculate the relative coordinates of the second marker */
        x = mTileView.getScrollX() + mTileView.getWidth() / 3 - mTileView.getOffsetX();
        y = mTileView.getScrollY() + mTileView.getHeight() * 2 / 3 - mTileView.getOffsetY();
        relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mTileView.getScale());
        relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mTileView.getScale());

        mSecondMarkerRelativeX = relativeX;
        mSecondMarkerRelativeY = relativeY;
    }

    private void updateDistanceView() {
        CoordinateTranslater translater = mTileView.getCoordinateTranslater();
        mDistanceView.updateLine(
                (float) translater.translateX(mFirstMarkerRelativeX),
                (float) translater.translateY(mFirstMarkerRelativeY),
                (float) translater.translateX(mSecondMarkerRelativeX),
                (float) translater.translateY(mSecondMarkerRelativeY));
    }

    private void prepareDistanceCalculation() {
        mDistanceThread = new HandlerThread("Distance calculation thread", Thread.MIN_PRIORITY);
        mDistanceThread.start();
        mHandler = new Handler(mDistanceThread.getLooper());
    }

    private void stopDistanceCalculation() {
        mDistanceThread.quit();
        mDistanceThread = null;
        mHandler = null;
    }

    public enum DistanceUnit {
        KM, METERS, MILES
    }

    public interface DistanceListener {
        void onDistance(float distance, DistanceUnit unit);
    }
}
