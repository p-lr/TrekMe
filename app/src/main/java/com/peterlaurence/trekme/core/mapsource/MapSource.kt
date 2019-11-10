package com.peterlaurence.trekme.core.mapsource

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


enum class MapSource {
    IGN, SWISS_TOPO, USGS, IGN_SPAIN, OPEN_STREET_MAP
}

@Parcelize
data class MapSourceBundle(val mapSource: MapSource) : Parcelable