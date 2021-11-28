package com.peterlaurence.trekme.features.map.domain.models

import com.peterlaurence.trekme.core.map.domain.Marker

data class MarkerWithNormalizedPos(val marker: Marker, val x: Double, val y: Double)
