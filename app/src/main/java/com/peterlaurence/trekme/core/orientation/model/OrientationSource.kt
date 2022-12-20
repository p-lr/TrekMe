package com.peterlaurence.trekme.core.orientation.model

import kotlinx.coroutines.flow.SharedFlow

interface OrientationSource {
    /**
     * The orientation is in radians.
     */
    val orientationFlow: SharedFlow<Double>
}