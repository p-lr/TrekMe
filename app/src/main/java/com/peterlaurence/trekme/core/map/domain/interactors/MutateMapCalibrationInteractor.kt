package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.CalibrationMethod
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.projection.UniversalTransverseMercator
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import javax.inject.Inject

class MutateMapCalibrationInteractor @Inject constructor(
    private val mapRepository: MapRepository,
    private val saveMapInteractor: SaveMapInteractor
) {
    /**
     * Mutate the [Projection] of a given [Map], then saves the [Map].
     */
    suspend fun mutateProjection(map: Map, projectionName: String?) = runCatching {
        val projectionType = projectionHashMap[projectionName]
        val projection = projectionType?.getDeclaredConstructor()?.newInstance()
        val oldConfig = map.configSnapshot
        val newConfig = oldConfig.copy(calibration = oldConfig.calibration?.copy(projection = projection))

        val newMap = map.copy(config = newConfig)
        mapRepository.notifyUpdate(map, newMap)
        saveMapInteractor.saveMap(newMap)
    }

    /**
     * Mutate the [CalibrationMethod] of a given [Map], then saves the [Map].
     */
    suspend fun mutateCalibrationMethod(map: Map, calibrationMethod: CalibrationMethod)= runCatching {
        val oldConfig = map.configSnapshot
        val newConfig = oldConfig.copy(calibration = oldConfig.calibration?.copy(calibrationMethod = calibrationMethod))

        val newMap = map.copy(config = newConfig)
        mapRepository.notifyUpdate(map, newMap)
        saveMapInteractor.saveMap(newMap)
    }

    /**
     * All [Projection]s are registered here.
     */
    private val projectionHashMap = object : HashMap<String, Class<out Projection>>() {
        init {
            put(MercatorProjection.NAME, MercatorProjection::class.java)
            put(UniversalTransverseMercator.NAME, UniversalTransverseMercator::class.java)
        }
    }
}