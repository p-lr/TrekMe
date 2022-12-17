package com.peterlaurence.trekme.core.map.data.models

import android.graphics.Bitmap
import com.peterlaurence.trekme.core.map.domain.models.*
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.projection.Projection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.*

/**
 * **Warning**: This class isn't thread-safe. It's advised to thread-confine the use of this
 * class to the main thread.
 */
class MapFileBased(
    private val config: MapConfig,
) : Map {
    /**
     * The unique id that identifies the [Map].
     * It can be used to identify the map across app restarts.
     */
    override val id: UUID = config.uuid

    override val name: String = config.name

    override val thumbnailImage: Bitmap?
        get() = config.thumbnailImage

    override val thumbnailSize = 256

    /**
     * Get the bounds the map. See [MapBounds]. By default, the size of the map is used for the
     * bounds. Bounds are updated after [calibrate] is invoked.
     */
    override var mapBounds =
        MapBounds(0.0, 0.0, config.size.width.toDouble(), config.size.height.toDouble())
        private set
    override val markers: MutableStateFlow<List<Marker>> = MutableStateFlow(emptyList())
    override val landmarks: MutableStateFlow<List<Landmark>> = MutableStateFlow(emptyList())
    override val beacons: MutableStateFlow<List<Beacon>> = MutableStateFlow(emptyList())

    override val routes = MutableStateFlow<List<Route>>(listOf())

    private val _elevationFix: MutableStateFlow<Int> = MutableStateFlow(config.elevationFix)

    /**
     * The calibration status is either :
     *  * [CalibrationStatus.OK]
     *  * [CalibrationStatus.NONE]
     *  * [CalibrationStatus.ERROR]
     */
    override var calibrationStatus = CalibrationStatus.NONE
        private set

    override val projectionName: String?
        get() {
            val cal = config.calibration
            if (cal != null) {
                val proj = cal.projection
                if (proj != null) return proj.name
            }
            return null
        }

    override val projection: Projection? = config.calibration?.projection

    override val sizeInBytes: Long?
        get() = config.sizeInBytes

    override fun setSizeInBytes(size: Long) {
        config.sizeInBytes = size
    }

    init {
        calibrate()
    }

    private fun calibrate() {
        val (projection, _, calibrationPoints) = config.calibration ?: return

        /* Init the projection */
        projection?.init()
        val newBounds = when (calibrationMethod) {
            CalibrationMethod.SIMPLE_2_POINTS -> if (calibrationPoints.size >= 2) {
                CalibrationMethods.simple2PointsCalibration(
                    calibrationPoints[0],
                    calibrationPoints[1]
                )
            } else null
            CalibrationMethod.CALIBRATION_3_POINTS -> if (calibrationPoints.size >= 3) {
                CalibrationMethods.calibrate3Points(
                    calibrationPoints[0],
                    calibrationPoints[1], calibrationPoints[2]
                )
            } else null
            CalibrationMethod.CALIBRATION_4_POINTS -> if (calibrationPoints.size == 4) {
                CalibrationMethods.calibrate4Points(
                    calibrationPoints[0],
                    calibrationPoints[1], calibrationPoints[2],
                    calibrationPoints[3]
                )
            } else null
        }

        if (newBounds != null) mapBounds = newBounds

        /* Update the calibration status */
        setCalibrationStatus()
    }

    private fun setCalibrationStatus() {
        // TODO : implement the detection of an erroneous calibration
        val cal = config.calibration
        calibrationStatus = if (cal != null && cal.calibrationPoints.size >= 2) {
            CalibrationStatus.OK
        } else {
            CalibrationStatus.NONE
        }
    }

    override fun setElevationFix(fix: Int) {
        _elevationFix.update { fix }
    }

    override fun getElevationFix(): StateFlow<Int> = _elevationFix

    override val levelList: List<Level>
        get() = config.levels

    override val calibrationMethod: CalibrationMethod
        get() {
            val cal = config.calibration
            return cal?.calibrationMethod ?: CalibrationMethod.SIMPLE_2_POINTS
        }

    override val origin: MapOrigin
        get() = config.origin

    /**
     * Get the number of calibration that should be defined.
     */
    override val calibrationPointsNumber: Int
        get() {
            return when (calibrationMethod) {
                CalibrationMethod.SIMPLE_2_POINTS -> 2
                CalibrationMethod.CALIBRATION_3_POINTS -> 3
                CalibrationMethod.CALIBRATION_4_POINTS -> 4
            }
        }

    override val calibrationPoints: List<CalibrationPoint>
        get() {
            val cal = config.calibration
            return cal?.calibrationPoints ?: emptyList()
        }

    override val imageExtension: String
        get() = config.imageExtension
    override val widthPx: Int
        get() = config.size.width
    override val heightPx: Int
        get() = config.size.height

    override val configSnapshot: MapConfig
        get() = config.copy()

    override fun copy(config: MapConfig): Map {
        return MapFileBased(config = config)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MapFileBased

        /* By design, only MapConfig participates in equals policy */
        if (config != other.config) return false

        return true
    }

    override fun hashCode(): Int {
        /* By design, only MapConfig participates in hashcode policy */
        return config.hashCode()
    }
}