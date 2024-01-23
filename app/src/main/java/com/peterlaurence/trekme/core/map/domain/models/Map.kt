package com.peterlaurence.trekme.core.map.domain.models

import android.graphics.Bitmap
import com.peterlaurence.trekme.core.projection.Projection
import kotlinx.coroutines.flow.*
import java.util.UUID

/**
 * A map contains all the information that defines a map. That includes:
 *
 *  * The name that will appear in the map choice list
 *  * The calibration method along with calibration points
 *  * Points of interest
 * ...
 *
 * A [Map] is typically created using a [MapConfig] object that includes information relative to
 * levels, the tile size, the name of the map, etc. This configuration is later accessible through
 * the [configSnapshot] property.
 */
interface Map {
    val id: UUID
    val name: MutableStateFlow<String>
    val thumbnail: MutableStateFlow<Bitmap?>
    val mapBounds: MapBounds
    val markers: MutableStateFlow<List<Marker>>
    val landmarks: MutableStateFlow<List<Landmark>>
    val beacons: MutableStateFlow<List<Beacon>>
    val routes: MutableStateFlow<List<Route>>
    val excursionRefs: MutableStateFlow<List<ExcursionRef>>
    val elevationFix: MutableStateFlow<Int>
    val sizeInBytes: MutableStateFlow<Long?>
    val projectionName: String?
    val calibrationStatus: CalibrationStatus
    val projection: Projection?
    val levelList: List<Level>
    val calibrationMethod: CalibrationMethod
    val origin: MapOrigin
    val calibrationPointsNumber: Int
    val calibrationPoints: List<CalibrationPoint>
    val imageExtension: String
    val widthPx: Int
    val heightPx: Int
    val missingTilesCount: MutableStateFlow<Long?> // lazy loaded

    val configSnapshot: MapConfig
    fun copy(config: MapConfig): Map
}