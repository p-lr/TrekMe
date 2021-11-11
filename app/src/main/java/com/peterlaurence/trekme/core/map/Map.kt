package com.peterlaurence.trekme.core.map

import com.peterlaurence.trekme.util.zipTask
import android.graphics.Bitmap
import com.peterlaurence.trekme.util.ZipProgressionListener
import android.graphics.BitmapFactory
import com.peterlaurence.trekme.core.map.domain.*
import com.peterlaurence.trekme.core.projection.Projection
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
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
 * To create a map, three parameters are needed:
 *
 * @param config the [MapConfig] object that includes information relative to levels,
 * the tile size, the name of the map, etc.
 * @param configFile the [File] for serialization.
 * @param thumbnail the [File] image for map customization.
 *
 * @author P.Laurence
 */
class Map(
    private val config: MapConfig,
    var configFile: File,
    thumbnail: File?
) {
    val configSnapshot: MapConfig
        get() = config.copy()

    val thumbnailSize = 256

    private var mImage: Bitmap? = getBitmapFromFile(thumbnail)

    /**
     * Get the bounds the map. See [MapBounds].
     */
    var mapBounds: MapBounds? = null
        private set
    private var _landmarks: MutableList<Landmark>? = null
    private var _markerList: MutableList<Marker>? = null
    private var _routeList: MutableList<Route>? = null

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

    var projection: Projection?
        get() = config.calibration?.projection
        set(projection) {
            val cal = config.calibration
            if (cal != null) {
                val newCal = cal.copy(projection = projection)
                config.calibration = newCal
            }
        }

    val sizeInBytes: Long?
        get() = config.sizeInBytes

    fun setSizeInBytes(size: Long) {
        config.sizeInBytes = size
    }

    /**
     * TODO: remove that method
     * Check whether the map contains a given location. It's the responsibility of the caller to
     * know whether projected coordinated or lat/lon should be used.
     *
     * @param x a projected coordinate, or longitude
     * @param y a projected coordinate, or latitude
     */
    fun containsLocation(x: Double, y: Double): Boolean {
        val mapBounds = this.mapBounds
        return if (mapBounds != null) {
            x >= mapBounds.X0 && x <= mapBounds.X1 && y <= mapBounds.Y0 && y >= mapBounds.Y1
        } else {
            false
        }
    }

    fun calibrate() {
        val (projection, _, calibrationPoints) = config.calibration ?: return

        /* Init the projection */
        projection?.init()
        when (calibrationMethod) {
            CalibrationMethod.SIMPLE_2_POINTS -> if (calibrationPoints.size >= 2) {
                /* Correct points if necessary */
                CalibrationMethods.sanityCheck2PointsCalibration(
                    calibrationPoints[0],
                    calibrationPoints[1]
                )
                mapBounds = CalibrationMethods.simple2PointsCalibration(
                    calibrationPoints[0],
                    calibrationPoints[1]
                )
            }
            CalibrationMethod.CALIBRATION_3_POINTS -> if (calibrationPoints.size >= 3) {
                mapBounds = CalibrationMethods.calibrate3Points(
                    calibrationPoints[0],
                    calibrationPoints[1], calibrationPoints[2]
                )
            }
            CalibrationMethod.CALIBRATION_4_POINTS -> if (calibrationPoints.size == 4) {
                mapBounds = CalibrationMethods.calibrate4Points(
                    calibrationPoints[0],
                    calibrationPoints[1], calibrationPoints[2],
                    calibrationPoints[3]
                )
            }
            else -> {
            }
        }

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
     * The [File] which is the folder containing the map.
     * When the directory changed (after e.g a rename), the config file must be updated.
     */
    var directory: File?
        get() = configFile.parentFile
        set(dir) {
            configFile = File(dir, MAP_FILENAME)
        }

    var name: String
        get() = config.name
        set(newName) {
            config.name = newName
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

    fun areLandmarksDefined(): Boolean {
        return _landmarks?.let { it.size > 0 } ?: false
    }

    /**
     * Routes are lazily loaded.
     */
    val routes: List<Route>?
        get() = _routeList

    fun setRoutes(routes: List<Route>) {
        _routeList = routes.toMutableList()
    }

    /**
     * Add a new route to the map.
     */
    fun addRoute(route: Route) {
        _routeList?.add(route)
    }

    fun replaceRoute(from: Route, to: Route) {
        _routeList?.indexOf(from)?.also { i ->
            if (i != -1) {
                _routeList?.removeAt(i)
                _routeList?.add(i, to)
            } else {
                _routeList?.add(to)
            }
        }
    }

    fun deleteRoute(route: Route) {
        _routeList?.remove(route)
    }

    var image: Bitmap?
        get() = mImage
        set(thumbnail) {
            mImage = thumbnail
            config.thumbnail = THUMBNAIL_NAME
        }

    val imageOutputStream: OutputStream?
        get() {
            val targetFile = File(directory, THUMBNAIL_NAME)
            return try {
                FileOutputStream(targetFile)
            } catch (e: FileNotFoundException) {
                null
            }
        }

    val levelList: List<Level>
        get() = config.levels

    var calibrationMethod: CalibrationMethod?
        get() {
            val cal = config.calibration
            return cal?.calibrationMethod
        }
        set(method) {
            if (method == null) return
            val cal = config.calibration
            if (cal != null) {
                val newCal = cal.copy(calibrationMethod = method)
                config.calibration = newCal
            }
        }

    val origin: MapOrigin
        get() = config.origin

    /**
     * Get the number of calibration that should be defined.
     */
    val calibrationPointsNumber: Int
        get() {
            val method = calibrationMethod ?: return 0
            return when (method) {
                CalibrationMethod.SIMPLE_2_POINTS -> 2
                CalibrationMethod.CALIBRATION_3_POINTS -> 3
                CalibrationMethod.CALIBRATION_4_POINTS -> 4
            }
        }

    /**
     * Get a copy of the calibration points.
     * This returns only a copy to ensure that no modification is made to the calibration points
     * through this call.
     */
    var calibrationPoints: List<CalibrationPoint>
        get() {
            val cal = config.calibration
            return cal?.calibrationPoints ?: emptyList()
        }
        set(points) {
            val cal = config.calibration
            if (cal != null) {
                val newCal = cal.copy(calibrationPoints = points)
                config.calibration = newCal
            }
        }

    fun addCalibrationPoint(point: CalibrationPoint) {
        val cal = config.calibration
        if (cal != null) {
            val newPoints: MutableList<CalibrationPoint> = ArrayList(cal.calibrationPoints)
            newPoints.add(point)
            val newCal = cal.copy(calibrationPoints = newPoints)
            config.calibration = newCal
        }
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

    /**
     * Two [Map] are considered identical if they have the same configuration file.
     */
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        if (other is Map) {
            return other.configFile == configFile
        }
        return false
    }

    override fun hashCode(): Int {
        return configFile.hashCode()
    }

    companion object {
        private const val THUMBNAIL_NAME = "image.jpg"
        private fun getBitmapFromFile(file: File?): Bitmap? {
            val bmOptions = BitmapFactory.Options()
            return if (file != null) {
                BitmapFactory.decodeFile(file.absolutePath, bmOptions)
            } else null
        }
    }
}