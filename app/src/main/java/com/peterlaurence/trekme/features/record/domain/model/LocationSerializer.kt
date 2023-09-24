package com.peterlaurence.trekme.features.record.domain.model

import com.peterlaurence.trekme.core.location.domain.model.Location

interface LocationsSerializer {
    suspend fun onLocation(location: Location)
    suspend fun pause()
}