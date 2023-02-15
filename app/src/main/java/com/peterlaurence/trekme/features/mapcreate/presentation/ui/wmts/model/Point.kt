package com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.peterlaurence.trekme.core.wmts.domain.model.Point as PointDomain

@Parcelize
data class Point(val X: Double, val Y: Double) : Parcelable

fun Point.toDomain(): PointDomain = PointDomain(X, Y)
fun PointDomain.toModel() = Point(X, Y)