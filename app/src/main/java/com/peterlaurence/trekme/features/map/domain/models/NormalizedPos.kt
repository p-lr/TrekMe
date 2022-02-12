package com.peterlaurence.trekme.features.map.domain.models

data class NormalizedPos(val x: Double, val y: Double)

fun NormalizedPos.inBounds(): Boolean = x >=0 && y >= 0
