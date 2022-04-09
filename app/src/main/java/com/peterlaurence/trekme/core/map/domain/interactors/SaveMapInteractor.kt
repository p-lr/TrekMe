package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.map.domain.dao.MapSaverDao
import com.peterlaurence.trekme.core.repositories.map.MapListUpdateRepository
import com.peterlaurence.trekme.core.map.Map
import javax.inject.Inject

class SaveMapInteractor @Inject constructor(
    private val mapSaver: MapSaverDao,
    private val mapListUpdateRepository: MapListUpdateRepository
) {
    /**
     * Save the content of a [Map], so the changes persist upon application restart.
     * Here, it writes to the corresponding json file.
     * Then, broadcasts to [MapListUpdateRepository].
     *
     * @param map The [Map] to save.
     */
    suspend fun saveMap(map: Map) {
        mapSaver.save(map)
        mapListUpdateRepository.notifyMapListUpdate()
    }
}