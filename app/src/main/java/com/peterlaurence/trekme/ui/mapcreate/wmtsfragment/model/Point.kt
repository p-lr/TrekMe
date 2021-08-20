package com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.peterlaurence.trekme.core.mapsource.wmts.Point as PointDomain

@Parcelize
data class Point(val X: Double, val Y: Double) : Parcelable

fun Point.toDomain(): PointDomain = PointDomain(X, Y)