package com.peterlaurence.trekme.viewmodel.mapcreate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.geocoding.GeoPlace
import com.peterlaurence.trekme.core.geocoding.GeocodingEngine
import kotlinx.coroutines.flow.Flow

class GeocodingViewModel : ViewModel() {
    private val geocodingEngine = GeocodingEngine(viewModelScope)

    val geoPlaceFlow: Flow<List<GeoPlace>?> = geocodingEngine.geoPlaceFlow

    fun search(placeName: String) {
        geocodingEngine.search(placeName)
    }
}