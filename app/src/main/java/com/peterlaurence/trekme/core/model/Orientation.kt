package com.peterlaurence.trekme.core.model

import kotlinx.coroutines.flow.SharedFlow

interface OrientationSource {
    val orientationFlow: SharedFlow<Double>
}