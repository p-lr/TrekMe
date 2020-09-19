package com.peterlaurence.trekme.model.providers.stream

import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.IgnSourceData
import com.peterlaurence.trekme.core.mapsource.WmtsSource
import com.peterlaurence.trekme.core.mapsource.MapSourceData
import com.peterlaurence.trekme.core.mapsource.OrdnanceSurveyData
import com.peterlaurence.trekme.core.providers.urltilebuilder.*

/**
 * This is the unique place of the app (excluding tests), where we create a [TileStreamProvider]
 * from a [WmtsSource].
 */
fun createTileStreamProvider(wmtsSource: WmtsSource, mapSourceData: MapSourceData): TileStreamProvider {
    return when (wmtsSource) {
        WmtsSource.IGN -> {
            val ignSourceData = mapSourceData as? IgnSourceData
                    ?: throw Exception("Missing API for IGN source")
            val urlTileBuilder = UrlTileBuilderIgn(ignSourceData.api, ignSourceData.layer.realName)
            TileStreamProviderIgn(urlTileBuilder, ignSourceData.layer)
        }
        WmtsSource.USGS -> {
            val urlTileBuilder = UrlTileBuilderUSGS()
            TileStreamProviderUSGS(urlTileBuilder)
        }
        WmtsSource.OPEN_STREET_MAP -> {
            val urlTileBuilder = UrlTileBuilderOSM()
            TileStreamProviderOSM(urlTileBuilder)
        }
        WmtsSource.IGN_SPAIN -> {
            val urlTileBuilder = UrlTileBuilderIgnSpain()
            TileStreamProviderIgnSpain(urlTileBuilder)
        }
        WmtsSource.SWISS_TOPO -> {
            val urlTileBuilder = UrlTileBuilderSwiss()
            TileStreamProviderSwiss(urlTileBuilder)
        }
        WmtsSource.ORDNANCE_SURVEY -> {
            val ordnanceSurveyData = mapSourceData as? OrdnanceSurveyData
                    ?: throw Exception("Missing API for Ordnance Survey source")
            val urlTileBuilder = UrlTileBuilderOrdnanceSurvey(ordnanceSurveyData.api)
            TileStreamProviderOrdnanceSurvey(urlTileBuilder)
        }
    }
}
