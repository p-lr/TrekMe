package com.peterlaurence.trekadvisor.core.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.lang.ref.WeakReference;

/**
 * A facility object that retrieves the orientation of the phone.
 *
 * @author peterLaurence on 09/04/16.
 */
public class OrientationSensor implements SensorEventListener {

    private WeakReference<OrientationListener> mOrientationListenerWeakReference;

    /* object internals */
    float[] orientationValues;
    float[] rMat;

    public OrientationSensor(Context context) {
        orientationValues = new float[3];
        rMat = new float[9];

        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_UI);
    }

    public interface OrientationListener {
        void onOrientation(int azimuth);
    }

    /**
     * Set a listener so that it will be called everytime the azimuth changes
     *
     * @param listener a {@link OrientationListener} object
     */
    public void setOrientationListener(OrientationListener listener) {
        mOrientationListenerWeakReference = new WeakReference<>(listener);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            /* Calculate the rotation matrix */
            SensorManager.getRotationMatrixFromVector(rMat, event.values);

            /* Get the azimuth value (orientation[0]) in degree */
            int mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientationValues)[0]) + 360) % 360;

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
}
