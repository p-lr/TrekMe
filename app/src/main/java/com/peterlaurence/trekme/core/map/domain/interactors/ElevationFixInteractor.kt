package com.peterlaurence.trekme.core.map.domain.interactors

import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.UpdateElevationFixDao
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class ElevationFixInteractor @Inject constructor(
    private val updateElevationFixDao: UpdateElevationFixDao
) {
    suspend fun setElevationFix(map: Map, fix: Int) {
        updateElevationFixDao.setElevationFix(map, fix).also { success ->
            if (success) {
                map.elevationFix.update { fix }
            }
        }
    }
}