package com.peterlaurence.trekme.core.excursion.domain.dao

import android.net.Uri
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import kotlinx.coroutines.flow.StateFlow

interface ExcursionDao {
    suspend fun getExcursionsFlow(): StateFlow<List<Excursion>>
    suspend fun getWaypoints(excursion: Excursion): List<ExcursionWaypoint>
    suspend fun getGeoRecordUri(excursion: Excursion): Uri?
}