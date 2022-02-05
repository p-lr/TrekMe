package com.peterlaurence.trekme.features.maplist.domain.interactors

import com.peterlaurence.trekme.core.map.domain.CalibrationPoint
import com.peterlaurence.trekme.features.maplist.domain.model.LatLonPoint
import com.peterlaurence.trekme.core.map.Map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CalibrationInteractor @Inject constructor() {
    suspend fun getLatLonForCalibrationPoint(
        calibrationPoint: CalibrationPoint,
        map: Map,
    ): LatLonPoint? = withContext(Dispatchers.Default) {
        val projection = map.projection
        if (projection != null) {
            val values = projection.undoProjection(calibrationPoint.absoluteX, calibrationPoint.absoluteY) ?: return@withContext null
            LatLonPoint(values[1], values[0])
        } else {
            LatLonPoint(calibrationPoint.absoluteY, calibrationPoint.absoluteX)
        }
    }
}