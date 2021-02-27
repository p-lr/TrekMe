package com.peterlaurence.trekme.ui.mapview;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import com.peterlaurence.trekme.core.map.Map;
import com.peterlaurence.trekme.core.projection.Projection;
import com.peterlaurence.trekme.ui.mapview.components.DistanceMarker;
import com.peterlaurence.trekme.ui.mapview.components.LineView;
import com.peterlaurence.trekme.ui.tools.TouchMoveListener;

import org.jetbrains.annotations.NotNull;

import ovh.plrapps.mapview.MapView;
import ovh.plrapps.mapview.ReferentialData;
import ovh.plrapps.mapview.ReferentialListener;
import ovh.plrapps.mapview.core.CoordinateTranslater;

import static com.peterlaurence.trekme.core.geotools.GeoToolsKt.distanceApprox;
import static ovh.plrapps.mapview.api.MarkerApiKt.addMarker;
import static ovh.plrapps.mapview.api.MarkerApiKt.moveMarker;
import static ovh.plrapps.mapview.api.MarkerApiKt.removeMarker;

/**
 * Shows two {@link DistanceMarker} and a {@link LineView}.
 *
 * @author P.Laurence on 17/06/17.
 */
public class DistanceLayer implements ReferentialListener {
    private HandlerThread mDistanceThread;
    private LimitedHandler mHandler;
    private final Context mContext;
    private DistanceMarker mDistanceMarkerFirst;
    private DistanceMarker mDistanceMarkerSecond;
    private LineView mLineView;
    private TouchMoveListener mFirstTouchMoveListener;
    private TouchMoveListener mSecondTouchMoveListener;
    private boolean mVisible;
    private final DistanceListener mDistanceListener;
    private MapView mMapView;
    private Map mMap;
    private boolean isInitialized = false;

    private double mFirstMarkerRelativeX;
    private double mFirstMarkerRelativeY;
    private double mSecondMarkerRelativeX;
    private double mSecondMarkerRelativeY;
    private ReferentialData mReferentialData;

    @Override
    public void onReferentialChanged(@NotNull ReferentialData referentialData) {
        mReferentialData = referentialData;
        if (mLineView != null) mLineView.onReferentialChanged(referentialData);
        if (mFirstTouchMoveListener != null)
            mFirstTouchMoveListener.onReferentialChanged(referentialData);
        if (mSecondTouchMoveListener != null)
            mSecondTouchMoveListener.onReferentialChanged(referentialData);
    }

    DistanceLayer(Context context, DistanceListener listener) {
        mContext = context;
        mVisible = false;
        mDistanceListener = listener;
    }

    public void init(Map map, MapView mapView) {
        mMap = map;
        mMapView = mapView;
        isInitialized = true;
    }

    public void destroy() {
        if (mMapView != null) mMapView.removeReferentialListener(this);
        if (mDistanceThread != null) mDistanceThread.quit();
    }

    public void toggle() {
        if (!mVisible) {
            show(getState());
        } else hide();
    }

    /**
     * Shows the two {@link DistanceMarker} and the {@link LineView}.<br>
     * {@link #init(Map, MapView)} must have been called before.
     */
    public void show(@NotNull State state) {
        if (!isInitialized) return;
        mMapView.addReferentialListener(this);

        /* Create the DistanceView (the line between the two markers) */
        mLineView = new LineView(mContext, mReferentialData);
        mLineView.onReferentialChanged(mReferentialData);
        mMapView.addView(mLineView);

        /* Setup the first marker */
        mDistanceMarkerFirst = new DistanceMarker(mContext);
        TouchMoveListener.MarkerMoveAgent firstMarkerMarkerMoveAgent = (mapView, view, x, y) -> {
            mFirstMarkerRelativeX = x;
            mFirstMarkerRelativeY = y;
            moveMarker(mapView, mDistanceMarkerFirst, x, y);
            onMarkerMoved();
        };
        mFirstTouchMoveListener = new TouchMoveListener(mMapView, firstMarkerMarkerMoveAgent);
        mFirstTouchMoveListener.onReferentialChanged(mReferentialData);
        mDistanceMarkerFirst.setOnTouchListener(mFirstTouchMoveListener);

        /* Setup the second marker*/
        mDistanceMarkerSecond = new DistanceMarker(mContext);
        TouchMoveListener.MarkerMoveAgent secondMarkerMarkerMoveAgent = new TouchMoveListener.MarkerMoveAgent() {
            @Override
            public void onMarkerMove(MapView mapView, View view, double x, double y) {
                mSecondMarkerRelativeX = x;
                mSecondMarkerRelativeY = y;
                moveMarker(mapView, mDistanceMarkerSecond, x, y);
                onMarkerMoved();
            }
        };
        mSecondTouchMoveListener = new TouchMoveListener(mMapView, secondMarkerMarkerMoveAgent);
        mSecondTouchMoveListener.onReferentialChanged(mReferentialData);
        mDistanceMarkerSecond.setOnTouchListener(mSecondTouchMoveListener);

        /* Set their positions - use default position if previous state was not visible */
        if (!state.visible) {
            initDistanceMarkers();
        } else {
            mFirstMarkerRelativeX = state.firstMarkerRelativeX;
            mFirstMarkerRelativeY = state.firstMarkerRelativeY;
            mSecondMarkerRelativeX = state.secondMarkerRelativeX;
            mSecondMarkerRelativeY = state.secondMarkerRelativeY;
        }

        /* ..and add them to the MapView */
        addMarker(mMapView, mDistanceMarkerFirst, mFirstMarkerRelativeX, mFirstMarkerRelativeY,
                -0.5f, -0.5f, 0f, 0f, null);
        addMarker(mMapView, mDistanceMarkerSecond, mSecondMarkerRelativeX, mSecondMarkerRelativeY,
                -0.5f, -0.5f, 0f, 0f, null);
        mVisible = true;

        /* Start the thread that will process distance calculations */
        prepareDistanceCalculation();

        /* Trigger the first computation */
        onMarkerMoved();
    }

    /**
     * Hide the two {@link DistanceMarker} and the {@link LineView}.
     */
    public void hide() {
        if (mMapView != null) {
            mMapView.removeReferentialListener(this);
            if (mDistanceMarkerFirst != null) {
                removeMarker(mMapView, mDistanceMarkerFirst);
            }
            if (mDistanceMarkerSecond != null) {
                removeMarker(mMapView, mDistanceMarkerSecond);
            }
            if (mLineView != null) {
                mMapView.removeView(mLineView);
            }
        }

        mDistanceMarkerFirst = null;
        mDistanceMarkerSecond = null;
        mLineView = null;
        mVisible = false;

        /* Stop the thread that process distance calculation */
        stopDistanceCalculation();

        mDistanceListener.hideDistance();
    }

    public boolean isVisible() {
        return mVisible;
    }

    @NotNull
    public State getState() {
        return new State(mVisible, mFirstMarkerRelativeX, mFirstMarkerRelativeY, mSecondMarkerRelativeX,
                mSecondMarkerRelativeY);
    }

    private void initDistanceMarkers() {
        /* Calculate the relative coordinates of the first marker */
        int x = mMapView.getScrollX() + (int) (mMapView.getWidth() * 0.66f) - mMapView.getOffsetX();
        int y = mMapView.getScrollY() + (int) (mMapView.getHeight() * 0.33f) - mMapView.getOffsetY();
        CoordinateTranslater coordinateTranslater = mMapView.getCoordinateTranslater();
        double relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mMapView.getScale());
        double relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mMapView.getScale());

        mFirstMarkerRelativeX = Math.min(relativeX, coordinateTranslater.getRight());
        mFirstMarkerRelativeY = Math.min(relativeY, coordinateTranslater.getTop());

        /* Calculate the relative coordinates of the second marker */
        x = mMapView.getScrollX() + (int) (mMapView.getWidth() * 0.33f) - mMapView.getOffsetX();
        y = mMapView.getScrollY() + (int) (mMapView.getHeight() * 0.66f) - mMapView.getOffsetY();
        relativeX = coordinateTranslater.translateAndScaleAbsoluteToRelativeX(x, mMapView.getScale());
        relativeY = coordinateTranslater.translateAndScaleAbsoluteToRelativeY(y, mMapView.getScale());

        mSecondMarkerRelativeX = Math.max(relativeX, coordinateTranslater.getLeft());
        mSecondMarkerRelativeY = Math.max(relativeY, coordinateTranslater.getBottom());
    }

    private void onMarkerMoved() {
        /* Update the ui */
        CoordinateTranslater translater = mMapView.getCoordinateTranslater();
        mLineView.updateLine(
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

        mHandler = new LimitedHandler(mDistanceThread.getLooper(), runnable);
    }

    private void stopDistanceCalculation() {
        if (mDistanceThread != null) {
            mDistanceThread.quit();
        }
        mDistanceThread = null;
        mHandler = null;
    }

    public interface DistanceListener {
        /**
         * @param distance the numeric value of the distance, in meters
         */
        void onDistance(float distance);

        void toggleDistanceVisibility();

        void showDistance();

        void hideDistance();
    }

    /**
     * A custom {@link Handler} that executes a {@link DistanceCalculationRunnable} at a maximum rate. <p>
     * To submit a new distance calculation, call {@link #submit(double, double, double, double)}.
     */
    private static class LimitedHandler extends Handler {
        private static final int DISTANCE_CALCULATION_TIMEOUT = 100;
        private static final int MESSAGE = 0;
        private final DistanceCalculationRunnable mDistanceRunnable;

        LimitedHandler(Looper looper, DistanceCalculationRunnable task) {
            super(looper);
            mDistanceRunnable = task;
        }

        void submit(double relativeX1, double relativeY1, double relativeX2, double relativeY2) {
            mDistanceRunnable.setPoints(relativeX1, relativeY1, relativeX2, relativeY2);
            if (!hasMessages(MESSAGE)) {
                sendEmptyMessageDelayed(MESSAGE, DISTANCE_CALCULATION_TIMEOUT);
            }
        }

        @Override
        public void handleMessage(@NotNull Message msg) {
            post(mDistanceRunnable);
        }
    }

    private static class DistanceCalculationRunnable implements Runnable {
        private final Map mMap;
        private final Handler mPostExecuteHandler;
        private final UpdateDistanceListenerRunnable mPostExecuteTask;
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
                distance = distanceApprox(mRelativeX1, mRelativeY1, mRelativeX2, mRelativeY2);
            } else {
                double[] firstPointGeographic = projection.undoProjection(mRelativeX1, mRelativeY1);
                if (firstPointGeographic == null) return;

                double[] secondPointGeographic = projection.undoProjection(mRelativeX2, mRelativeY2);
                if (secondPointGeographic == null) return;

                double lat1 = firstPointGeographic[1];
                double lon1 = firstPointGeographic[0];
                double lat2 = secondPointGeographic[1];
                double lon2 = secondPointGeographic[0];
                distance = distanceApprox(lat1, lon1, lat2, lon2);
            }
            mPostExecuteTask.setDistance(distance);

            mPostExecuteHandler.post(mPostExecuteTask);
        }
    }

    private static class UpdateDistanceListenerRunnable implements Runnable {
        private double mDistance;
        private final DistanceListener mDistanceListener;

        UpdateDistanceListenerRunnable(DistanceListener listener) {
            mDistanceListener = listener;
        }

        void setDistance(double distance) {
            mDistance = distance;
        }

        @Override
        public void run() {
            mDistanceListener.onDistance((float) mDistance);
        }
    }

    public static class State implements Parcelable {
        boolean visible;
        double firstMarkerRelativeX;
        double firstMarkerRelativeY;
        double secondMarkerRelativeX;
        double secondMarkerRelativeY;

        State(boolean visible, double x1, double y1, double x2, double y2) {
            this.visible = visible;
            firstMarkerRelativeX = x1;
            firstMarkerRelativeY = y1;
            secondMarkerRelativeX = x2;
            secondMarkerRelativeY = y2;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            int visibleInt = visible ? 1 : 0;
            out.writeInt(visibleInt);
            out.writeDouble(firstMarkerRelativeX);
            out.writeDouble(firstMarkerRelativeY);
            out.writeDouble(secondMarkerRelativeX);
            out.writeDouble(secondMarkerRelativeY);
        }

        public static final Parcelable.Creator<State> CREATOR
                = new Parcelable.Creator<State>() {
            public State createFromParcel(Parcel in) {
                return new State(in);
            }

            public State[] newArray(int size) {
                return new State[size];
            }
        };

        private State(Parcel in) {
            int visibleInt = in.readInt();
            visible = visibleInt == 1;
            firstMarkerRelativeX = in.readDouble();
            firstMarkerRelativeY = in.readDouble();
            secondMarkerRelativeX = in.readDouble();
            secondMarkerRelativeY = in.readDouble();
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }
}
