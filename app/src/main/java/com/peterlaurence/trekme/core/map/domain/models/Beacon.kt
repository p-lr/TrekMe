package com.peterlaurence.trekme.core.map.domain.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Beacon(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val radius: Float = 50f,
    val comment: String = ""
): Parcelable
