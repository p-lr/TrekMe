package com.peterlaurence.trekme.features.common.domain.interactors

import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.core.map.domain.dao.ExcursionRefDao
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * For instance, excursions are imported into [Map]s using classic routes. However, this is only
 * done in-memory. Eventually, new data structures specific to excursions will be introduced, to
 * get rid of the current limitations (a route == track segment).
 */
class MapExcursionInteractor @Inject constructor(
    private val excursionRefDao: ExcursionRefDao,
    private val excursionDao: ExcursionDao,
    private val excursionRepository: ExcursionRepository,
    private val mapRepository: MapRepository,
) {
    suspend fun importExcursions(map: Map) {
        excursionRefDao.importExcursionRefs(map, excursionProvider = excursionRepository::getExcursion)
    }

    /**
     * Save the color in the "#AARRGGBB" format.
     */
    suspend fun setColor(map: Map, ref: ExcursionRef, color: Long) {
        ref.color.value = '#' + java.lang.Long.toHexString(color)
        excursionRefDao.saveExcursionRef(map, ref)
    }

    suspend fun rename(ref: ExcursionRef, newName: String) {
        excursionDao.rename(ref.id, newName)
    }

    suspend fun removeExcursionOnMap(map: Map, ref: ExcursionRef) {
        excursionRefDao.removeExcursionRef(map, ref)
    }

    /**
     * By design the [ExcursionRef].id is the same as the excursion id.
     */
    suspend fun removeExcursionOnMaps(excursionId: String) {
        mapRepository.getCurrentMapList().forEach { map ->
            excursionRefDao.removeExcursionRef(map, excursionRefId = excursionId)
        }
    }

    suspend fun toggleVisibility(map: Map, ref: ExcursionRef) {
        ref.visible.update { !it }
        excursionRefDao.saveExcursionRef(map, ref)
    }

    suspend fun setVisibility(map: Map, ref: ExcursionRef, visibility: Boolean) {
        ref.visible.update { visibility }
        excursionRefDao.saveExcursionRef(map, ref)
    }

    suspend fun createExcursionRef(map: Map, excursionId: String) {
        val excursion = excursionRepository.getExcursion(excursionId)
        if (excursion != null) {
            excursionRefDao.createExcursionRef(map, excursion)
        }
    }

    suspend fun setAllExcursionVisibility(map: Map, newVisibility: Boolean) = coroutineScope {
        map.excursionRefs.value.forEach {
            it.visible.update { newVisibility }
            excursionRefDao.saveExcursionRef(map, it)
        }
    }
}