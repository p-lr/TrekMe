package com.peterlaurence.trekme.core.mapsource

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


enum class MapSource {
    IGN, SWISS_TOPO, OPEN_STREET_MAP, USGS, IGN_SPAIN
}

@Parcelize
data class MapSourceBundle(val mapSource: MapSource) : Parcelable