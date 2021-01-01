package com.peterlaurence.trekme.core.geocoding.backend

import com.peterlaurence.trekme.core.geocoding.GeoPlace

interface GeocodingBackend {
    suspend fun search(query: String): List<GeoPlace>?
}