package com.peterlaurence.trekme.model.providers.stream

import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.IgnSourceData
import com.peterlaurence.trekme.core.mapsource.MapSource
import com.peterlaurence.trekme.core.mapsource.MapSourceData
import com.peterlaurence.trekme.core.mapsource.OrdnanceSurveyData
import com.peterlaurence.trekme.core.providers.urltilebuilder.*

/**
 * This is the unique place of the app (excluding tests), where we create a [TileStreamProvider]
 * from a [MapSource].
 */
fun createTileStreamProvider(mapSource: MapSource, mapSourceData: MapSourceData): TileStreamProvider {
    return when (mapSource) {
        MapSource.IGN -> {
            val ignSourceData = mapSourceData as? IgnSourceData
                    ?: throw Exception("Missing API for IGN source")
            val urlTileBuilder = UrlTileBuilderIgn(ignSourceData.api, ignSourceData.layer.realName)
            TileStreamProviderIgn(urlTileBuilder, ignSourceData.layer)
        }
        MapSource.USGS -> {
            val urlTileBuilder = UrlTileBuilderUSGS()
            TileStreamProviderUSGS(urlTileBuilder)
        }
        MapSource.OPEN_STREET_MAP -> {
            val urlTileBuilder = UrlTileBuilderOSM()
            TileStreamProviderOSM(urlTileBuilder)
        }
        MapSource.IGN_SPAIN -> {
            val urlTileBuilder = UrlTileBuilderIgnSpain()
            TileStreamProviderIgnSpain(urlTileBuilder)
        }
        MapSource.SWISS_TOPO -> {
            val urlTileBuilder = UrlTileBuilderSwiss()
            TileStreamProviderSwiss(urlTileBuilder)
        }
        MapSource.ORDNANCE_SURVEY -> {
            val ordnanceSurveyData = mapSourceData as? OrdnanceSurveyData
                    ?: throw Exception("Missing API for Ordnance Survey source")
            val urlTileBuilder = UrlTileBuilderOrdnanceSurvey(ordnanceSurveyData.api)
            TileStreamProviderOrdnanceSurvey(urlTileBuilder)
        }
    }
}
