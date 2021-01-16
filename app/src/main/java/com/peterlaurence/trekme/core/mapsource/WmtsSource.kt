package com.peterlaurence.trekme.core.mapsource

import android.os.Parcelable
import com.peterlaurence.trekme.core.providers.layers.Layer
import com.peterlaurence.trekme.repositories.mapcreate.LayerProperties
import kotlinx.parcelize.Parcelize


enum class WmtsSource {
    IGN, SWISS_TOPO, OPEN_STREET_MAP, USGS, IGN_SPAIN, ORDNANCE_SURVEY
}

@Parcelize
data class WmtsSourceBundle(val wmtsSource: WmtsSource, val levelMin: Int = 1, val levelMax: Int = 18, val startMaxLevel: Int = 16) : Parcelable

sealed class MapSourceData
data class IgnSourceData(val api: String, val layer: Layer, val overlays: List<LayerProperties>) : MapSourceData()
data class OrdnanceSurveyData(val api: String): MapSourceData()
data class OsmSourceData(val layer: Layer): MapSourceData()
object NoData : MapSourceData()