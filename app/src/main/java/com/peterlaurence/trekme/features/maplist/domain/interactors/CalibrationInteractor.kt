package com.peterlaurence.trekme.features.maplist.domain.interactors

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.models.CalibrationPoint
import com.peterlaurence.trekme.core.map.domain.interactors.SaveMapInteractor
import com.peterlaurence.trekme.features.maplist.domain.model.CalibrationData
import com.peterlaurence.trekme.features.maplist.domain.model.LatLonPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CalibrationInteractor @Inject constructor(
    private val saveMapInteractor: SaveMapInteractor
) {
    /**
     * Given a [CalibrationPoint], get its latitude and longitude.
     */
    suspend fun getLatLonForCalibrationPoint(
        calibrationPoint: CalibrationPoint,
        map: Map,
    ): LatLonPoint? = withContext(Dispatchers.Default) {
        val projection = map.projection
        if (projection != null) {
            val values =
                projection.undoProjection(calibrationPoint.absoluteX, calibrationPoint.absoluteY)
                    ?: return@withContext null
            LatLonPoint(values[1], values[0])
        } else {
            LatLonPoint(calibrationPoint.absoluteY, calibrationPoint.absoluteX)
        }
    }

    /**
     * Update the calibration, providing the lat, lon, and normalized coordinates of each
     * calibration point.
     */
    suspend fun updateCalibration(
        calibrationDataList: List<CalibrationData>,
        map: Map
    ): Boolean {
        val projection = map.projection
        val newCalibrationPoints = calibrationDataList.map { data ->
            if (projection != null) {
                val projectedValues = projection.doProjection(data.lat, data.lon)
                if (projectedValues != null) {
                    CalibrationPoint(data.x, data.y, projectedValues[0], projectedValues[1])
                } else return false
            } else {
                CalibrationPoint(data.x, data.y, data.lon, data.lat)
            }
        }

        /* Update the calibration in memory */
        withContext(Dispatchers.Main) {
            map.calibrationPoints = newCalibrationPoints
            map.calibrate()
        }

        /* Effectively save the map (and consequently, the calibration) */
        saveMapInteractor.saveMap(map)
        return true
    }
}