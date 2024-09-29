package com.peterlaurence.trekme.core.map.data.dao

import com.peterlaurence.trekme.core.map.domain.dao.CheckTileStreamProviderDao
import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider
import com.peterlaurence.trekme.core.map.data.models.BitmapProvider
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource

class CheckTileStreamProviderDaoImpl : CheckTileStreamProviderDao {
    override suspend fun check(wmtsSource: WmtsSource, tileStreamProvider: TileStreamProvider): Boolean {
        val bitmapProvider = BitmapProvider(tileStreamProvider)
        return when (wmtsSource) {
            WmtsSource.IGN -> bitmapProvider.getBitmap(1, 1, 1) != null
            WmtsSource.SWISS_TOPO -> bitmapProvider.getBitmap(180, 266, 9) != null
            WmtsSource.OPEN_STREET_MAP -> bitmapProvider.getBitmap(1, 1, 1) != null
            WmtsSource.USGS -> bitmapProvider.getBitmap(1, 1, 1) != null
            WmtsSource.IGN_SPAIN -> bitmapProvider.getBitmap(24, 31, 6) != null
            WmtsSource.ORDNANCE_SURVEY -> bitmapProvider.getBitmap(40, 61, 7) != null
            WmtsSource.IGN_BE -> bitmapProvider.getBitmap(172, 262, 9) != null
        }
    }
}