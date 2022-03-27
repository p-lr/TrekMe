package com.peterlaurence.trekme.core.lib.geocoding.backend

import com.peterlaurence.trekme.core.lib.geocoding.GeoPlace

interface GeocodingBackend {
    suspend fun search(query: String): List<GeoPlace>?
}