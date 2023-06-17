package com.peterlaurence.trekme.core.excursion.domain.dao

import android.net.Uri
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import kotlinx.coroutines.flow.StateFlow

interface ExcursionDao {
    suspend fun getExcursionsFlow(): StateFlow<List<Excursion>>
    suspend fun getWaypoints(excursion: Excursion): List<ExcursionWaypoint>
    suspend fun getGeoRecord(excursion: Excursion): GeoRecord?
    fun getGeoRecordUri(id: String): Uri?
    suspend fun putExcursion(id: String, title: String, type: ExcursionType, description: String, geoRecord: GeoRecord): Boolean
    suspend fun deleteExcursions(ids: List<String>): Boolean
    suspend fun rename(id: String, newName: String): Boolean
}