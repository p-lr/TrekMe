package com.peterlaurence.trekme.core.geocoding.domain.model

import com.peterlaurence.trekme.core.geocoding.domain.engine.GeoPlace

interface GeocodingBackend {
    suspend fun search(query: String): List<GeoPlace>?
}