package com.peterlaurence.trekme.core.events;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;


/**
 * A facility object that retrieves the orientation of the phone.
 *
 * @author peterLaurence on 09/04/16.
 */
public class OrientationEventManager implements SensorEventListener {

    private OrientationListener mOrientationListener;

    private HandlerThread mOrientationThread;
    private LimitedHandler mHandler;
    private boolean mStarted;
    private Activity mActivity;


    public OrientationEventManager(Activity context) {
        mActivity = context;
        mStarted = false;

        start();
    }

    /**
     * Set a listener so that it will be called everytime the azimuth changes
     *
     * @param listener a {@link OrientationListener} object
     */
    public void setOrientationListener(OrientationListener listener) {
        mOrientationListener = listener;
        if (mStarted) {
            listener.onOrientationEnable();
        } else {
            listener.onOrientationDisable();
        }
    }

    public boolean toggleOrientation() {
        if (mStarted) {
            stop();
        } else {
            start();
        }
        return mStarted;
    }

    public boolean isStarted() {
        return mStarted;
    }

    public void start() {
        /* If no listener, no need to go further */
        if (mOrientationListener == null) return;

        mOrientationThread = new HandlerThread("Orientation calculation thread", Thread.MIN_PRIORITY);
        mOrientationThread.start();

        /* Create a handler on the ui thread */
        Handler handler = new Handler(Looper.getMainLooper());

        /* This runnable will be executed on ui thread after each distance calculation */
        UpdateOrientationListenerRunnable updateUiRunnable = new UpdateOrientationListenerRunnable(mOrientationListener);

        /* The task to be executed on the dedicated thread */
        OrientationCalculationRunnable runnable = new OrientationCalculationRunnable(mActivity, handler, updateUiRunnable);

        mHandler = new LimitedHandler(mOrientationThread.getLooper(), runnable);

        /* Subscribe to orientation events */
        SensorManager sensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
        Sensor mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);

        /* Then update the listener (view) */
        if (mOrientationListener != null) {
            mOrientationListener.onOrientationEnable();
        }

        mStarted = true;
    }

    public void stop() {
        /* First update the listener (view) */
        if (mOrientationListener != null) {
            mOrientationListener.onOrientationDisable();
        }

        /* Then unsubscribe */
        SensorManager sensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(this);

        /* Stop dedicated calculation thread */
        if (mOrientationThread != null) {
            mOrientationThread.quit();
        }
        mOrientationThread = null;
        mHandler = null;

        mStarted = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        /* Schedule orientation calculation */
        if (mHandler != null) {
            mHandler.submit(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public interface OrientationListener {
        void onOrientation(int azimuth);

        void onOrientationEnable();

        void onOrientationDisable();
    }

    /**
     * A custom {@link Handler} that executes a {@link OrientationCalculationRunnable} at a maximum rate. <p>
     * To submit a new orientation calculation, call {@link #submit(SensorEvent)}.
     */
    private static class LimitedHandler extends Handler {
        private static final int ORIENTATION_CALCULATION_TIMEOUT = 32;
        private static final int MESSAGE = 0;
        private OrientationCalculationRunnable mOrientationRunnable;

        LimitedHandler(Looper looper, OrientationCalculationRunnable task) {
            super(looper);
            mOrientationRunnable = task;
        }

        void submit(SensorEvent event) {
            mOrientationRunnable.setSensorValues(event.values);
            if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR && !hasMessages(MESSAGE)) {
                sendEmptyMessageDelayed(MESSAGE, ORIENTATION_CALCULATION_TIMEOUT);
            }
        }

        @Override
        public void handleMessage(Message msg) {
            post(mOrientationRunnable);
        }
    }

    /**
     * Process the values from the {@code Sensor.TYPE_ROTATION_VECTOR}. <br>
     * This objects holds a reference to an {@link Activity}. Be careful not to hold a reference to
     * it upon configuration change.
     */
    private static class OrientationCalculationRunnable implements Runnable {
        private Activity mActivity;
        private Handler mPostExecuteHandler;
        private UpdateOrientationListenerRunnable mPostExecuteTask;
        private transient float[] mValues;
        private float[] mOrientationValues;
        private float[] mrMat;

        OrientationCalculationRunnable(Activity activity, Handler postExecuteHandler,
                                       UpdateOrientationListenerRunnable postExecuteTask) {
            mOrientationValues = new float[3];
            mrMat = new float[9];
            mActivity = activity;
            mPostExecuteHandler = postExecuteHandler;
            mPostExecuteTask = postExecuteTask;
        }

        void setSensorValues(float[] values) {
            mValues = values;
        }

        @Override
        public void run() {
            /* Calculate the rotation matrix */
            SensorManager.getRotationMatrixFromVector(mrMat, mValues);

            int screenRotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
            int fix = 0;
            switch (screenRotation) {
                case Surface.ROTATION_90:
                    fix = 90;
                    break;

                case Surface.ROTATION_180:
                    fix = 180;
                    break;

                case Surface.ROTATION_270:
                    fix = 270;
                    break;

                default:
                    break;
            }

            /* Get the azimuth value (orientation[0]) in degree */
            int azimuth = (int) (Math.toDegrees(SensorManager.getOrientation(mrMat, mOrientationValues)[0]) + 360 + fix) % 360;

            /* Post task on ui thread */
            mPostExecuteTask.setAzimuth(azimuth);
            mPostExecuteHandler.post(mPostExecuteTask);
        }
    }

    /**
     * This task is executed on ui thread after each orientation calculation.
     */
    private static class UpdateOrientationListenerRunnable implements Runnable {
        private int mAzimuth;
        private OrientationListener mOrientationListener;

        UpdateOrientationListenerRunnable(OrientationListener listener) {
            mOrientationListener = listener;
        }

        void setAzimuth(int azimuth) {
            mAzimuth = azimuth;
        }

        @Override
        public void run() {
            if (mOrientationListener != null) {
                mOrientationListener.onOrientation(mAzimuth);
            }
        }
    }
}
