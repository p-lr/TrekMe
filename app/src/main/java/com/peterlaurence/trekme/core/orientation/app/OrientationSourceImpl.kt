package com.peterlaurence.trekme.core.orientation.app

import android.content.Context
import android.hardware.*
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.core.orientation.model.OrientationSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs

class OrientationSourceImpl(
    appContext: Context
) : OrientationSource {
    private val sensorManager: SensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorReading = FloatArray(3)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    override val orientationFlow: SharedFlow<Double> by lazy {
        makeFlow()
    }

    private fun makeFlow(): SharedFlow<Double> {
        return callbackFlow {
            var started = true

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                        System.arraycopy(
                            event.values,
                            0,
                            rotationVectorReading,
                            0,
                            rotationVectorReading.size
                        )
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                }
            }

            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.also { sensor ->
                sensorManager.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_FASTEST
                )
            }

            launch {
                while (started) {
                    delay(1)
                    updateOrientation()

                    /* Get the azimuth value (orientation[0]) in radians */
                    val azimuth = orientationAngles[0].toDouble()

                    send(azimuth)
                }
            }
            awaitClose {
                started = false
                sensorManager.unregisterListener(listener)
            }
        }.distinctUntilChanged { old, new ->
            abs(old - new) < 0.002
        }.flowOn(
            Dispatchers.Default
        ).shareIn(
            ProcessLifecycleOwner.get().lifecycleScope,
            SharingStarted.WhileSubscribed()
        )
    }

    private fun updateOrientation() {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVectorReading)

        /* Get the azimuth value (orientation[0]) in degree */
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
    }

}