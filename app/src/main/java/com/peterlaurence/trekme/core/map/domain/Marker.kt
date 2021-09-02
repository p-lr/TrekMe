package com.peterlaurence.trekme.core.map.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Marker(
    var lat: Double,
    var lon: Double,
    var name: String? = null,
    var elevation: Double? = null,
    var proj_x: Double? = null,
    var proj_y: Double? = null,
    var comment: String? = null
) : Parcelable