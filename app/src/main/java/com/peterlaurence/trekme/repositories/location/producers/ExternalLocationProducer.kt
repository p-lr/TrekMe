package com.peterlaurence.trekme.repositories.location.producers

import com.peterlaurence.trekme.core.model.Location
import com.peterlaurence.trekme.core.model.LocationProducer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ExternalLocationProducer : LocationProducer {
    override val locationFlow: Flow<Location>
        get() = flow { } // Do nothing for now
}