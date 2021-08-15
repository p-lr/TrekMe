package com.peterlaurence.trekme.core.providers.stream

import com.peterlaurence.trekme.core.map.TileStreamProvider
import com.peterlaurence.trekme.core.mapsource.*
import com.peterlaurence.trekme.core.providers.urltilebuilder.*

/**
 * This is the unique place of the app (excluding tests), where we create a [TileStreamProvider]
 * from a [MapSourceData].
 */
fun newTileStreamProvider(data: MapSourceData): TileStreamProvider {
    return when (data) {
        is IgnSourceData -> {
            val urlTileBuilder = UrlTileBuilderIgn(data.api, data.layer)
            val primaryTileStreamProvider = TileStreamProviderIgn(urlTileBuilder, data.layer)
            if (data.overlays.isEmpty()) {
                primaryTileStreamProvider
            } else {
                val tileStreamOverlays = data.overlays.map {
                    val ts = TileStreamProviderIgn(UrlTileBuilderIgn(data.api, it.layer), it.layer)
                    TileStreamWithAlpha(ts, it.opacity)
                }
                TileStreamProviderOverlay(primaryTileStreamProvider, tileStreamOverlays)
            }
        }
        UsgsData -> {
            val urlTileBuilder = UrlTileBuilderUSGS()
            TileStreamProviderUSGS(urlTileBuilder)
        }
        is OsmSourceData -> {
            val urlTileBuilder = UrlTileBuilderOSM(data.layer.id)
            TileStreamProviderOSM(urlTileBuilder)
        }
        is IgnSpainData -> {
            val urlTileBuilder = UrlTileBuilderIgnSpain()
            TileStreamProviderIgnSpain(urlTileBuilder)
        }
        is SwissTopoData -> {
            val urlTileBuilder = UrlTileBuilderSwiss()
            TileStreamProviderSwiss(urlTileBuilder)
        }
        is OrdnanceSurveyData -> {
            val urlTileBuilder = UrlTileBuilderOrdnanceSurvey(data.api)
            TileStreamProviderOrdnanceSurvey(urlTileBuilder)
        }
    }
}
