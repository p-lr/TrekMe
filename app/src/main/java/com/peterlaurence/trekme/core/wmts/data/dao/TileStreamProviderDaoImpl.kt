package com.peterlaurence.trekme.core.wmts.data.dao

import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider
import com.peterlaurence.trekme.core.wmts.data.provider.TileStreamProviderIgn
import com.peterlaurence.trekme.core.wmts.data.provider.TileStreamProviderIgnSpain
import com.peterlaurence.trekme.core.wmts.data.provider.TileStreamProviderOSM
import com.peterlaurence.trekme.core.wmts.data.provider.TileStreamProviderOrdnanceSurvey
import com.peterlaurence.trekme.core.wmts.data.provider.TileStreamProviderOverlay
import com.peterlaurence.trekme.core.wmts.data.provider.TileStreamProviderSwiss
import com.peterlaurence.trekme.core.wmts.data.provider.TileStreamProviderUSGS
import com.peterlaurence.trekme.core.wmts.data.provider.TileStreamWithAlpha
import com.peterlaurence.trekme.core.wmts.data.urltilebuilder.UrlTileBuilderIgn
import com.peterlaurence.trekme.core.wmts.data.urltilebuilder.UrlTileBuilderIgnSpain
import com.peterlaurence.trekme.core.wmts.data.urltilebuilder.UrlTileBuilderOSM
import com.peterlaurence.trekme.core.wmts.data.urltilebuilder.UrlTileBuilderOrdnanceSurvey
import com.peterlaurence.trekme.core.wmts.data.urltilebuilder.UrlTileBuilderSwiss
import com.peterlaurence.trekme.core.wmts.data.urltilebuilder.UrlTileBuilderUSGS
import com.peterlaurence.trekme.core.wmts.domain.dao.TileStreamProviderDao
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSpainData
import com.peterlaurence.trekme.core.wmts.domain.model.MapSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.OrdnanceSurveyData
import com.peterlaurence.trekme.core.wmts.domain.model.OsmSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.SwissTopoData
import com.peterlaurence.trekme.core.wmts.domain.model.UsgsData
import com.peterlaurence.trekme.features.common.data.dao.IgnApiDao
import com.peterlaurence.trekme.features.common.data.dao.OrdnanceSurveyApiDao

class TileStreamProviderDaoImpl constructor(
    private val ignApiDao: IgnApiDao,
    private val ordnanceSurveyApiDao: OrdnanceSurveyApiDao,
): TileStreamProviderDao {
    override suspend fun newTileStreamProvider(data: MapSourceData): Result<TileStreamProvider> {
        val tileStreamProvider = when (data) {
            is IgnSourceData -> {
                val ignApi =
                    ignApiDao.getApi() ?: return Result.failure(Exception("IGN Api fetch error"))
                val urlTileBuilder = UrlTileBuilderIgn(ignApi, data.layer)
                val primaryTileStreamProvider = TileStreamProviderIgn(urlTileBuilder, data.layer)
                if (data.overlays.isEmpty()) {
                    primaryTileStreamProvider
                } else {
                    val tileStreamOverlays = data.overlays.map {
                        val ts =
                            TileStreamProviderIgn(UrlTileBuilderIgn(ignApi, it.layer), it.layer)
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
                val api = ordnanceSurveyApiDao.getApi()
                    ?: return Result.failure(Exception("Ordnance survey Api fetch error"))
                val urlTileBuilder = UrlTileBuilderOrdnanceSurvey(api)
                TileStreamProviderOrdnanceSurvey(urlTileBuilder)
            }
        }

        return Result.success(tileStreamProvider)
    }
}