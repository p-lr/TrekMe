package com.peterlaurence.trekme.core.sensors

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlin.math.abs

class OrientationSensor(val activity: Activity) : SensorEventListener {
    private val sensorManager: SensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    var isStarted: Boolean = false
        private set

    fun getAzimuthFlow(): Flow<Float> = flow {
        while (isStarted) {
            delay(1)
            updateOrientation()
            val screenRotation: Int = activity.windowManager.defaultDisplay.rotation
            var fix = 0
            when (screenRotation) {
                Surface.ROTATION_90 -> fix = 90
                Surface.ROTATION_180 -> fix = 180
                Surface.ROTATION_270 -> fix = 270
            }

            /* Get the azimuth value (orientation[0]) in degree */
            val azimuth = (Math.toDegrees(orientationAngles[0].toDouble()) + 360 + fix).toFloat() % 360

            emit(azimuth)
        }
    }.distinctUntilChanged { old, new -> abs(old - new) < 0.1 }.flowOn(Dispatchers.Default)

    fun start() {
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.also { sensor ->
            sensorManager.registerListener(
                    this,
                    sensor,
                    SensorManager.SENSOR_DELAY_FASTEST
            )
        }
        isStarted = true
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        isStarted = false
    }

    fun toggle(): Boolean {
        if (isStarted) stop() else start()
        return isStarted
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            System.arraycopy(event.values, 0, rotationVectorReading, 0, rotationVectorReading.size)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    private fun updateOrientation() {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVectorReading)

        /* Get the azimuth value (orientation[0]) in degree */
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
    }
}
