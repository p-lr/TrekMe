package com.peterlaurence.trekme.core.orientation

import kotlinx.coroutines.flow.SharedFlow

interface OrientationSource {
    val orientationFlow: SharedFlow<Double>
}