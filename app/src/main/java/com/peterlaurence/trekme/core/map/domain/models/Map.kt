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
    val creationData: CreationData?
    val missingTilesCount: MutableStateFlow<Long?> // lazy loaded
    val lastRepairDate: MutableStateFlow<Long?>
    val lastUpdateDate: MutableStateFlow<Long?>
    val isDownloadPending: MutableStateFlow<Boolean>
}