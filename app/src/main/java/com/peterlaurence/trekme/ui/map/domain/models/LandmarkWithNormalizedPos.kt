package com.peterlaurence.trekme.ui.map.domain.models

import com.peterlaurence.trekme.core.map.domain.Landmark

data class LandmarkWithNormalizedPos(val landmark: Landmark, val x: Double, val y: Double)