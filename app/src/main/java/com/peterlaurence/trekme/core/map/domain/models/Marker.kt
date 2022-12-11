package com.peterlaurence.trekme.core.map.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

@Parcelize
data class Marker(
    val id: String = UUID.randomUUID().toString(),
    val lat: Double,
    val lon: Double,
    val name: String = "",
    val elevation: Double? = null,
    val time: Long? = null,
    val comment: String = ""
) : Parcelable