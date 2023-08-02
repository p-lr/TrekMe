package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.settings.Settings
import java.util.UUID
import javax.inject.Inject

class SetMapInteractor @Inject constructor(
    private val mapRepository: MapRepository,
    private val settings: Settings
) {
    suspend fun setMap(mapId: UUID) {
        val map = mapRepository.getMap(mapId) ?: return
        mapRepository.setCurrentMap(map)

        /* Remember this map so the app can directly start on this map */
        settings.setLastMapId(map.id)
    }
}