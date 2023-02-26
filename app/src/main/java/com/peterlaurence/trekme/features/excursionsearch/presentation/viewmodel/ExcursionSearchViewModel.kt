package com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionCategory
import com.peterlaurence.trekme.core.geocoding.domain.engine.GeoPlace
import com.peterlaurence.trekme.core.geocoding.domain.repository.GeocodingRepository
import com.peterlaurence.trekme.features.excursionsearch.domain.interactor.ExcursionInteractor
import com.peterlaurence.trekme.features.excursionsearch.presentation.model.ExcursionCategoryChoice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExcursionSearchViewModel @Inject constructor(
    private val geocodingRepository: GeocodingRepository,
    private val excursionInteractor: ExcursionInteractor
): ViewModel() {

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

    fun onSearch(useCurrentLocation: Boolean, excursionCategoryChoice: ExcursionCategoryChoice) {
        println("xxxxx searching for ${selectedGeoPlace.value?.name} at ${selectedGeoPlace.value?.locality} | use current loc $useCurrentLocation | choice $excursionCategoryChoice")
        val place = selectedGeoPlace.value ?: return
        viewModelScope.launch {
            val result = excursionInteractor.search(place.lat, place.lon, excursionCategoryChoice)
            println("xxxxxx received ${result.size} results")
        }
    }
}