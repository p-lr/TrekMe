package com.peterlaurence.trekme.features.record.domain.interactors

import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.map.domain.models.Route
import com.peterlaurence.trekme.di.DefaultDispatcher
import com.peterlaurence.trekme.features.common.domain.model.ElevationSource
import com.peterlaurence.trekme.features.common.domain.model.ElevationSourceInfo
import com.peterlaurence.trekme.features.record.domain.model.ElevationData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UpdateGeoRecordElevationsInteractor @Inject constructor(
    private val excursionDao: ExcursionDao,
    @DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher
) {
    suspend fun updateElevations(elevationData: ElevationData) {
        val updatedGeoRecord = updateGpxFileWithTrustedElevations(elevationData) ?: return
        excursionDao.updateGeoRecord(elevationData.id, updatedGeoRecord)
    }

    private suspend fun updateGpxFileWithTrustedElevations(eleData: ElevationData): GeoRecord? {
        val segmentElePoints = eleData.segmentElePoints

        /* Safeguard */
        if (segmentElePoints.isEmpty()) return null

        return withContext(defaultDispatcher) {
            val newRouteGroups = eleData.geoRecord.routeGroups.mapIndexed { iRoute, routeGroup ->
                if (iRoute == 0) {
                    val routes = routeGroup.routes.zip(segmentElePoints).map { (route, segmentElePt) ->
                        if (route.routeMarkers.size == segmentElePt.points.size) {
                            val newRouteMarkers = route.routeMarkers.zip(segmentElePt.points).map {
                                it.first.copy(elevation = it.second.elevation)
                            }
                            Route(route.id, route.name.value, initialMarkers = newRouteMarkers, elevationTrusted = true)
                        } else route
                    }
                    routeGroup.copy(routes = routes)
                } else routeGroup
            }

            val newElevationSourceInfo = ElevationSourceInfo(ElevationSource.IGN_RGE_ALTI, eleData.sampling)

            eleData.geoRecord.copy(routeGroups = newRouteGroups, elevationSourceInfo = newElevationSourceInfo)
        }
    }
}