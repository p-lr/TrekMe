package com.peterlaurence.trekme.core.orientation

import kotlinx.coroutines.flow.SharedFlow

interface OrientationSource {
    /**
     * The orientation is in radians.
     */
    val orientationFlow: SharedFlow<Double>
}