package com.peterlaurence.trekme.features.map.domain.models

import com.peterlaurence.trekme.core.map.domain.models.Beacon

data class BeaconWithNormalizedPos(val beacon: Beacon, val x: Double, val y: Double)