package com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionCategory
import com.peterlaurence.trekme.core.geocoding.domain.engine.GeoPlace
import com.peterlaurence.trekme.core.geocoding.domain.repository.GeocodingRepository
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.features.excursionsearch.domain.interactor.ExcursionInteractor
import com.peterlaurence.trekme.features.excursionsearch.presentation.model.ExcursionCategoryChoice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExcursionSearchViewModel @Inject constructor(
    private val geocodingRepository: GeocodingRepository,
    private val excursionInteractor: ExcursionInteractor,
    private val locationSource: LocationSource
) : ViewModel() {

    val geoPlaceFlow = geocodingRepository.geoPlaceFlow
    val isGeoPlaceLoading = geocodingRepository.isLoadingFlow

    val selectedGeoPlace = MutableStateFlow<GeoPlace?>(null)

    init {
        viewModelScope.launch {
            /* By default, always select the first GeoPlace - the user can change it later on */
            geoPlaceFlow.collect {
                val firstPlace = it.firstOrNull()
                if (firstPlace != null) {
                    selectedGeoPlace.value = firstPlace
                }
            }
        }
    }

    fun onQueryTextSubmit(query: String) {
        if (query.isNotEmpty()) {
            geocodingRepository.search(query)
        }
    }

    fun getExcursionCategories(): Array<ExcursionCategory> {
        return ExcursionCategory.values()
    }

    fun onSearchWithPlace(excursionCategoryChoice: ExcursionCategoryChoice) {
        val place = selectedGeoPlace.value ?: return
        viewModelScope.launch {
            excursionInteractor.search(place.lat, place.lon, excursionCategoryChoice)
        }
    }

    fun searchWithLocation(excursionCategoryChoice: ExcursionCategoryChoice) {
        viewModelScope.launch {
            val location = locationSource.locationFlow.firstOrNull() ?: return@launch
            excursionInteractor.search(
                location.latitude,
                location.longitude,
                excursionCategoryChoice
            )
        }
    }
}