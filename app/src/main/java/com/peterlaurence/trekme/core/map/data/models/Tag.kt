package com.peterlaurence.trekme.core.map.data.models

import com.peterlaurence.trekme.core.wmts.domain.model.IgnClassic
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.MapSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.OsmAndHd
import com.peterlaurence.trekme.core.wmts.domain.model.OsmSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.Outdoors

val ignTag = byteArrayOf(0x1b, 0x1c)
val osmHdStandardTag = byteArrayOf(0x1c, 0x1c)

fun makeTag(mapSourceData: MapSourceData): ByteArray? {
    return when (mapSourceData) {
        is IgnSourceData -> if (mapSourceData.layer == IgnClassic) ignTag else null
        is OsmSourceData -> when (mapSourceData.layer) {
            OsmAndHd, Outdoors -> osmHdStandardTag
            else -> null
        }
        else -> null
    }
}

