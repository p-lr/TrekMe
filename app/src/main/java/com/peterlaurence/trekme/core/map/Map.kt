package com.peterlaurence.trekme.core.map

import com.peterlaurence.trekme.util.zipTask
import android.graphics.Bitmap
import com.peterlaurence.trekme.util.ZipProgressionListener
import com.peterlaurence.trekme.core.map.domain.models.*
import com.peterlaurence.trekme.core.projection.Projection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.OutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * A map contains all the information that defines a map. That includes :
 *
 *  * The name that will appear in the map choice list
 *  * The directory that contains image data and configuration file
 *  * The calibration method along with calibration points
 *  * Points of interest
 * ...
 *
 * **Warning**: This class isn't thread-safe. It's advised to thread-confine the use of this
 * class to the main thread.
 *
 * To create a map, two parameters are required:
 *
 * @param config the [MapConfig] object that includes information relative to levels,
 * the tile size, the name of the map, etc.
 * @param configFile the [File] for serialization.
 */
data class Map(
    private val config: MapConfig,
    val configFile: File,
    val thumbnailImage: Bitmap? = null
) {
    val configSnapshot: MapConfig
        get() = config.copy()

    /**
     * The [File] which is the folder containing the map.
     * When the directory changed (after e.g a rename), the config file must be updated.
     */
    val directory: File?
        get() = configFile.parentFile

    val name: String = config.name

    val thumbnailSize = 256

    /**
     * Get the bounds the map. See [MapBounds]. By default, the size of the map is used for the
     * bounds. Bounds are updated after [calibrate] is invoked.
     */
    var mapBounds = MapBounds(0.0, 0.0, config.size.width.toDouble(), config.size.height.toDouble())
        private set
    private var _landmarks: MutableList<Landmark>? = null
    private var _markerList: MutableList<Marker>? = null
    private val _routes = MutableStateFlow<List<Route>>(listOf())

    /**
     * The calibration status is either :
     *  * [CalibrationStatus.OK]
     *  * [CalibrationStatus.NONE]
     *  * [CalibrationStatus.ERROR]
     */
    var calibrationStatus = CalibrationStatus.NONE
        private set

    val projectionName: String?
        get() {
            val cal = config.calibration
            if (cal != null) {
                val proj = cal.projection
                if (proj != null) return proj.name
            }
            return null
        }

    val projection: Projection? = config.calibration?.projection

    val sizeInBytes: Long?
        get() = config.sizeInBytes

    fun setSizeInBytes(size: Long) {
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

    /**
     * Markers are lazily loaded.
     */
    val markers: List<Marker>?
        get() = _markerList

    fun setMarkers(markers: List<Marker>) {
        _markerList = markers.toMutableList()
    }

    fun addMarker(marker: Marker) {
        _markerList?.add(marker)
    }

    fun deleteMarker(marker: Marker) {
        _markerList?.remove(marker)
    }

    val landmarks: List<Landmark>?
        get() = _landmarks

    fun setLandmarks(landmarks: List<Landmark>) {
        _landmarks = landmarks.toMutableList()
    }

    fun addLandmark(landmark: Landmark) {
        _landmarks?.add(landmark)
    }

    fun deleteLandmark(landmark: Landmark) {
        _landmarks?.remove(landmark)
    }

    val routes: StateFlow<List<Route>> = _routes.asStateFlow()

    fun setRoutes(routes: List<Route>) {
        _routes.value = routes
    }

    /**
     * Add a new route to the map.
     */
    fun addRoute(route: Route) {
        _routes.value = _routes.value + route
    }

    fun replaceRoute(from: Route, to: Route) {
        _routes.value = _routes.value.map {
            if (it == from) to else it
        }
    }

    fun deleteRoute(route: Route) {
        _routes.value = _routes.value - route
    }



    val levelList: List<Level>
        get() = config.levels


    private val _calibrationMethodStateFlow = MutableStateFlow(calibrationMethod)
    val calibrationMethodStateFlow = _calibrationMethodStateFlow.asStateFlow()

    val calibrationMethod: CalibrationMethod
        get() {
            val cal = config.calibration
            return cal?.calibrationMethod ?: CalibrationMethod.SIMPLE_2_POINTS
        }

    val origin: MapOrigin
        get() = config.origin

    /**
     * Get the number of calibration that should be defined.
     */
    val calibrationPointsNumber: Int
        get() {
            return when (calibrationMethod) {
                CalibrationMethod.SIMPLE_2_POINTS -> 2
                CalibrationMethod.CALIBRATION_3_POINTS -> 3
                CalibrationMethod.CALIBRATION_4_POINTS -> 4
            }
        }

    val calibrationPoints: List<CalibrationPoint>
        get() {
            val cal = config.calibration
            return cal?.calibrationPoints ?: emptyList()
        }

    val imageExtension: String
        get() = config.imageExtension
    val widthPx: Int
        get() = config.size.width
    val heightPx: Int
        get() = config.size.height

    /**
     * The unique id that identifies the [Map]. It can later be used to retrieve the
     * [Map] instance from the map loader.
     */
    val id: Int
        get() = configFile.path.hashCode()

    /**
     * Archives the map.
     *
     * Creates a zip file named with this [Map] name and the date. This file is placed in the
     * parent folder of the [Map].
     * Beware that this is a blocking call and should be executed from inside a background thread.
     */
    fun zip(listener: ZipProgressionListener, outputStream: OutputStream) {
        val mapFolder = configFile.parentFile
        if (mapFolder != null) {
            zipTask(mapFolder, outputStream, listener)
        }
    }

    fun generateNewNameWithDate(): String {
        val date = Date()
        val dateFormat: DateFormat = SimpleDateFormat("dd\\MM\\yyyy-HH:mm:ss", Locale.ENGLISH)
        return name + "-" + dateFormat.format(date)
    }

    enum class CalibrationStatus {
        OK, NONE, ERROR
    }
}