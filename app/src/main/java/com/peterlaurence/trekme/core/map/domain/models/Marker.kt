package com.peterlaurence.trekme.core.map.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Marker(
    var lat: Double,
    var lon: Double,
    var name: String? = null,
    var elevation: Double? = null,
    val time: Long? = null,
    var comment: String? = null
) : Parcelable