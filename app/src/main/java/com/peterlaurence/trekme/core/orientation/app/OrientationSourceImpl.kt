package com.peterlaurence.trekme.core.orientation.app

import android.content.Context
import android.hardware.*
import com.peterlaurence.trekme.core.orientation.model.OrientationSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlin.math.abs

class OrientationSourceImpl(
    private val scope: CoroutineScope,
    appContext: Context,
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

                        updateOrientation()

                        /* Get the azimuth value (orientation[0]) in radians */
                        val azimuth = orientationAngles[0].toDouble()

                        this@callbackFlow.trySend(azimuth)
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

            awaitClose {
                sensorManager.unregisterListener(listener)
            }
        }.distinctUntilChanged { old, new ->
            abs(old - new) < 0.002
        }.flowOn(
            Dispatchers.Default
        ).shareIn(
            scope,
            SharingStarted.WhileSubscribed()
        )
    }

    private fun updateOrientation() {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVectorReading)

        /* Get the azimuth value (orientation[0]) in degree */
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
    }

}