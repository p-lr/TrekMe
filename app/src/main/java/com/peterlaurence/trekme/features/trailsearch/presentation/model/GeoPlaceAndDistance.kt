package com.peterlaurence.trekme.features.trailsearch.presentation.model

import com.peterlaurence.trekme.core.geocoding.domain.engine.GeoPlace

data class GeoPlaceAndDistance(val geoPlace: GeoPlace, val distance: Double?)
