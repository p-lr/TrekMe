package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.map.domain.dao.MapSaverDao
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.repositories.map.MapRepository
import javax.inject.Inject

class SaveMapInteractor @Inject constructor(
    private val mapSaver: MapSaverDao,
    private val mapRepository: MapRepository
) {
    /**
     * Save the content of a [Map], so the changes persist upon application restart.
     * Here, it writes to the corresponding json file.
     *
     * @param map The [Map] to save.
     */
    suspend fun saveMap(map: Map) {
        mapSaver.save(map)
    }

    suspend fun addAndSaveMap(map: Map) {
        mapRepository.addMaps(listOf(map))
        mapSaver.save(map)
    }
}