package com.peterlaurence.trekme.features.common.domain.interactors

import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.map.domain.dao.ExcursionRefDao
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * For instance, excursions are imported into [Map]s using classic routes. However, this is only
 * done in-memory. Eventually, new data structures specific to excursions will be introduced, to
 * get rid of the current limitations (a route == track segment).
 */
class MapExcursionInteractor @Inject constructor(
    private val excursionRefDao: ExcursionRefDao,
) {
    suspend fun importExcursions(map: Map) {
        excursionRefDao.importExcursionRefs(map)
    }

    /**
     * Save the color in the "#AARRGGBB" format.
     */
    suspend fun setColor(map: Map, ref: ExcursionRef, color: Long) {
        ref.color.value = '#' + java.lang.Long.toHexString(color)
        excursionRefDao.saveExcursionRef(map, ref)
    }

    suspend fun rename(map: Map, ref: ExcursionRef, newName: String) {
        ref.name.value = newName
        excursionRefDao.saveExcursionRef(map, ref)
    }

    suspend fun removeExcursionOnMap(map: Map, ref: ExcursionRef) {
        map.excursionRefs.update {
            it.filter { r -> r.id != ref.id }
        }
        excursionRefDao.removeExcursionRef(ref)
    }

    suspend fun toggleVisibility(map: Map, ref: ExcursionRef) {
        ref.visible.update { !it }
        excursionRefDao.saveExcursionRef(map, ref)
    }

    suspend fun createExcursionRef(map: Map, excursion: Excursion) {
        excursionRefDao.createExcursionRef(map, excursion)
    }
}