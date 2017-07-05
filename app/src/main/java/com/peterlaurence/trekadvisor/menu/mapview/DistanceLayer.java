package com.peterlaurence.trekadvisor.menu.mapview;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.View;

import com.peterlaurence.trekadvisor.core.geotools.GeoTools;
import com.peterlaurence.trekadvisor.core.map.Map;
import com.peterlaurence.trekadvisor.core.projection.Projection;
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
    private HandlerThread mDistanceThread;
    private LimitedHandler mHandler;
    private Context mContext;
    private DistanceMarker mDistanceMarkerFirst;
    private DistanceMarker mDistanceMarkerSecond;
    private DistanceView mDistanceView;
    private boolean mVisible;
    private DistanceListener mDistanceListener;
    private TileViewExtended mTileView;
    private Map mMap;

    private double mFirstMarkerRelativeX;
    private double mFirstMarkerRelativeY;
    private double mSecondMarkerRelativeX;
    private double mSecondMarkerRelativeY;

    DistanceLayer(Context context, DistanceListener listener) {
        mContext = context;
        mVisible = false;
        mDistanceListener = listener;
    }

    public void init(Map map, TileViewExtended tileView) {
        mMap = map;
        mTileView = tileView;
    }

    /**
     * Shows the two {@link DistanceMarker} and the {@link DistanceView}.<br>
     * {@link #init(Map, TileViewExtended)} must have been called before.
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
                onMarkerMoved();
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
                onMarkerMoved();
            }
        };
        MarkerTouchMoveListener secondMarkerTouchMoveListener = new MarkerTouchMoveListener(mTileView, secondMarkerMoveCallback);
        mDistanceMarkerSecond.setOnTouchListener(secondMarkerTouchMoveListener);

        /* Set their positions */
        initDistanceMarkers();
        onMarkerMoved();

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

    private void onMarkerMoved() {
        /* Update the ui */
        CoordinateTranslater translater = mTileView.getCoordinateTranslater();
        mDistanceView.updateLine(
                (float) translater.translateX(mFirstMarkerRelativeX),
                (float) translater.translateY(mFirstMarkerRelativeY),
                (float) translater.translateX(mSecondMarkerRelativeX),
                (float) translater.translateY(mSecondMarkerRelativeY));

        /* Schedule distance calculation */
        if (mHandler != null) {
            mHandler.submit(mFirstMarkerRelativeX, mFirstMarkerRelativeY,
                    mSecondMarkerRelativeX, mSecondMarkerRelativeY);
        }
    }

    private void prepareDistanceCalculation() {
        mDistanceThread = new HandlerThread("Distance calculation thread", Thread.MIN_PRIORITY);
        mDistanceThread.start();

        /* Get a handler on the ui thread */
        Handler handler = new Handler(Looper.getMainLooper());

        /* This runnable will be executed on ui thread after each distance calculation */
        UpdateDistanceListenerRunnable updateUiRunnable = new UpdateDistanceListenerRunnable(mDistanceListener);

        /* The task to be executed on the dedicated thread */
        DistanceCalculationRunnable runnable = new DistanceCalculationRunnable(mMap, handler, updateUiRunnable);

        mHandler = new LimitedHandler(runnable);
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

    /**
     * A custom {@link Handler} that executes a {@link DistanceCalculationRunnable} at a maximum rate. <p>
     * To submit a new distance calculation, call {@link #submit(double, double, double, double)}.
     */
    private static class LimitedHandler extends Handler {
        private static final int DISTANCE_CALCULATION_TIMEOUT = 100;
        private static final int MESSAGE = 0;
        private DistanceCalculationRunnable mDistanceRunnable;

        LimitedHandler(DistanceCalculationRunnable task) {
            mDistanceRunnable = task;
        }

        void submit(double relativeX1, double relativeY1, double relativeX2, double relativeY2) {
            mDistanceRunnable.setPoints(relativeX1, relativeY1, relativeX2, relativeY2);
            if (!hasMessages(MESSAGE)) {
                sendEmptyMessageDelayed(MESSAGE, DISTANCE_CALCULATION_TIMEOUT);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            mDistanceRunnable.run();
        }
    }

    private static class DistanceCalculationRunnable implements Runnable {
        private Map mMap;
        private Handler mPostExecuteHandler;
        private UpdateDistanceListenerRunnable mPostExecuteTask;
        private volatile double mRelativeX1;
        private volatile double mRelativeY1;
        private volatile double mRelativeX2;
        private volatile double mRelativeY2;

        DistanceCalculationRunnable(Map map, Handler postExecuteHandler, UpdateDistanceListenerRunnable postExecuteTask) {
            mMap = map;
            mPostExecuteHandler = postExecuteHandler;
            mPostExecuteTask = postExecuteTask;
        }

        void setPoints(double relativeX1, double relativeY1, double relativeX2, double relativeY2) {
            mRelativeX1 = relativeX1;
            mRelativeY1 = relativeY1;
            mRelativeX2 = relativeX2;
            mRelativeY2 = relativeY2;
        }

        /**
         * If the {@link Map} has no projection, the provided relative coordinates are expected to
         * be the wgs84 (latitude/longitude) coordinates.
         */
        @Override
        public void run() {
            double distance;
            Projection projection = mMap.getProjection();
            if (projection == null) {
                distance = GeoTools.distanceApprox(mRelativeX1, mRelativeY1, mRelativeX2, mRelativeY2);
            } else {
                double[] firstPointGeographic = projection.undoProjection(mRelativeX1, mRelativeY1);
                if (firstPointGeographic == null) return;

                double[] secondPointGeographic = projection.undoProjection(mRelativeX2, mRelativeY2);
                if (secondPointGeographic == null) return;

                double lat1 = firstPointGeographic[1];
                double lon1 = firstPointGeographic[0];
                double lat2 = secondPointGeographic[1];
                double lon2 = secondPointGeographic[0];
                distance = GeoTools.distanceApprox(lat1, lon1, lat2, lon2);
            }
            mPostExecuteTask.setDistance(distance);

            mPostExecuteHandler.post(mPostExecuteTask);
        }
    }

    private static class UpdateDistanceListenerRunnable implements Runnable {
        private double mDistance;
        private DistanceListener mDistanceListener;

        UpdateDistanceListenerRunnable(DistanceListener listener) {
            mDistanceListener = listener;
        }

        void setDistance(double distance) {
            mDistance = distance;
        }

        @Override
        public void run() {
            mDistanceListener.onDistance((float) mDistance, DistanceUnit.METERS);
        }
    }
}
