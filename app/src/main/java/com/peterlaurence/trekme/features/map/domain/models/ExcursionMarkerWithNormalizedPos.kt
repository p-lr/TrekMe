package com.peterlaurence.trekme.features.map.domain.models

import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint


data class ExcursionWaypointWithNormalizedPos(val waypoint: ExcursionWaypoint, val x: Double, val y: Double)
