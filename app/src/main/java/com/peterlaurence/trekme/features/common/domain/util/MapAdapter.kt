package com.peterlaurence.trekme.features.common.domain.util

import android.graphics.Bitmap
import com.peterlaurence.trekme.core.map.domain.models.Beacon
import com.peterlaurence.trekme.core.map.domain.models.CalibrationMethod
import com.peterlaurence.trekme.core.map.domain.models.CalibrationPoint
import com.peterlaurence.trekme.core.map.domain.models.CalibrationStatus
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Landmark
import com.peterlaurence.trekme.core.map.domain.models.Level
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.MapBounds
import com.peterlaurence.trekme.core.map.domain.models.MapConfig
import com.peterlaurence.trekme.core.map.domain.models.MapOrigin
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

/**
 * Some UI components work directly with a [Map] instance.
 * This adapter ease the creation of a [Map] instance, for preview purposes.
 */
fun makeMapForComposePreview(name: String = "Example map"): Map {
    return object : Map {
        override val id: UUID
            get() = UUID.fromString("someid")
        override val name: MutableStateFlow<String> = MutableStateFlow(name)
        override val thumbnail: MutableStateFlow<Bitmap?> = MutableStateFlow(null)
        override val mapBounds: MapBounds
            get() = TODO()
        override val markers: MutableStateFlow<List<Marker>>
            get() = MutableStateFlow(emptyList())
        override val landmarks: MutableStateFlow<List<Landmark>>
            get() = MutableStateFlow(emptyList())
        override val beacons: MutableStateFlow<List<Beacon>>
            get() = MutableStateFlow(emptyList())
        override val routes: MutableStateFlow<List<Route>>
            get() = MutableStateFlow(emptyList())
        override val excursionRefs: MutableStateFlow<List<ExcursionRef>>
            get() = MutableStateFlow(emptyList())
        override val elevationFix: MutableStateFlow<Int>
            get() = MutableStateFlow(0)
        override val sizeInBytes: MutableStateFlow<Long?>
            get() = TODO()
        override val projectionName: String?
            get() = projection?.name
        override val calibrationStatus: CalibrationStatus
            get() = CalibrationStatus.OK
        override val projection: Projection?
            get() = MercatorProjection()
        override val levelList: List<Level>
            get() = emptyList()
        override val calibrationMethod: CalibrationMethod
            get() = CalibrationMethod.SIMPLE_2_POINTS
        override val origin: MapOrigin
            get() = TODO()
        override val calibrationPointsNumber: Int
            get() = 0
        override val calibrationPoints: List<CalibrationPoint>
            get() = emptyList()
        override val imageExtension: String
            get() = ".jpg"
        override val widthPx: Int
            get() = 1000
        override val heightPx: Int
            get() = 1000
        override val configSnapshot: MapConfig
            get() = TODO()

        override fun copy(config: MapConfig): Map {
            TODO()
        }
    }
}