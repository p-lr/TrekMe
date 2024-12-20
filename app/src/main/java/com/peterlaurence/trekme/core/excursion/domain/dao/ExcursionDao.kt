package com.peterlaurence.trekme.core.excursion.domain.dao

import android.net.Uri
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionType
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecordExportFormat
import kotlinx.coroutines.flow.StateFlow

interface ExcursionDao {
    suspend fun getExcursionsFlow(): StateFlow<List<Excursion>>
    suspend fun getGeoRecord(excursion: Excursion): GeoRecord?
    suspend fun getGeoRecordUri(id: String, format: GeoRecordExportFormat): Uri?
    suspend fun putExcursion(id: String, title: String, type: ExcursionType, description: String, geoRecord: GeoRecord, isPathEditable: Boolean): Boolean
    suspend fun putExcursion(id: String, uri: Uri): Excursion?
    suspend fun deleteExcursions(ids: List<String>): Boolean
    suspend fun rename(id: String, newName: String): Boolean
    suspend fun updateGeoRecord(id: String, geoRecord: GeoRecord): Boolean
    suspend fun migrateLegacyRecordingsToExcursions() // For migration only
    suspend fun initWaypoints(excursion: Excursion)
    suspend fun updateWaypoint(excursion: Excursion, waypoint: ExcursionWaypoint, newLat: Double, newLon: Double)
    suspend fun updateWaypoint(excursion: Excursion, waypoint: ExcursionWaypoint, name: String?, lat: Double?, lon: Double?, comment: String?, color: String?)
    suspend fun updateWaypointsColor(excursion: Excursion, waypoints: List<ExcursionWaypoint>, color: String?)
    suspend fun deleteWaypoint(excursion: Excursion, waypoint: ExcursionWaypoint)
    suspend fun deleteWaypoints(excursion: Excursion, waypoints: List<ExcursionWaypoint>)
}