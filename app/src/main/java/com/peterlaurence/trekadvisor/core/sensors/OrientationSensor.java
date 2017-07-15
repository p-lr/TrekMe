package com.peterlaurence.trekadvisor.core.sensors;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Surface;

import java.lang.ref.WeakReference;

/**
 * A facility object that retrieves the orientation of the phone.
 *
 * @author peterLaurence on 09/04/16.
 */
public class OrientationSensor implements SensorEventListener {

    private WeakReference<OrientationListener> mOrientationListenerWeakReference;

    /* object internals */
    private boolean mStarted;
    private Activity mActivity;
    private float[] orientationValues;
    private float[] rMat;


    public OrientationSensor(Activity context) {
        mActivity = context;
        orientationValues = new float[3];
        rMat = new float[9];

        start();
    }

    /**
     * Set a listener so that it will be called everytime the azimuth changes
     *
     * @param listener a {@link OrientationListener} object
     */
    public void setOrientationListener(OrientationListener listener) {
        mOrientationListenerWeakReference = new WeakReference<>(listener);
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
        mStarted = true;

        /* First subscribe to orientation events */
        SensorManager sensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
        Sensor mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(this, mSensor, 100);

        /* Then update the listener (view) */
        if (mOrientationListenerWeakReference != null) {
            OrientationListener listener = mOrientationListenerWeakReference.get();
            if (listener != null) {
                listener.onOrientationEnable();
            }
        }
    }

    public void stop() {
        mStarted = false;

        /* First update the listener (view) */
        if (mOrientationListenerWeakReference != null) {
            OrientationListener listener = mOrientationListenerWeakReference.get();
            if (listener != null) {
                listener.onOrientationDisable();
            }
        }

        /* Then unsubscribe */
        SensorManager sensorManager = (SensorManager) mActivity.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            /* Calculate the rotation matrix */
            SensorManager.getRotationMatrixFromVector(rMat, event.values);

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
            int mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientationValues)[0]) + 360 + fix) % 360;

            /* Call the listener */
            if (mOrientationListenerWeakReference != null) {
                OrientationListener listener = mOrientationListenerWeakReference.get();
                if (listener != null) {
                    listener.onOrientation(mAzimuth);
                }
            }
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
}
