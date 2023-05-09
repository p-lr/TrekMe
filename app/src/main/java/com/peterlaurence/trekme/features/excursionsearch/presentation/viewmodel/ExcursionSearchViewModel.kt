package com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionCategory
import com.peterlaurence.trekme.core.geocoding.domain.engine.GeoPlace
import com.peterlaurence.trekme.core.geocoding.domain.repository.GeocodingRepository
import com.peterlaurence.trekme.features.excursionsearch.domain.repository.PendingSearchRepository
import com.peterlaurence.trekme.features.excursionsearch.presentation.model.ExcursionCategoryChoice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExcursionSearchViewModel @Inject constructor(
    private val geocodingRepository: GeocodingRepository,
    private val pendingSearchRepository: PendingSearchRepository,
) : ViewModel() {

    /* We don't persist search result in the domain. However, we create a state in the view-model.
     * If the user navigates to the map and goes back to the search, the previous search result
     * is still in memory because the view-model is scoped to the navigation graph. */
    val geoPlaceFlow: StateFlow<List<GeoPlace>> = geocodingRepository.geoPlaceFlow.stateIn(
        viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
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
        pendingSearchRepository.queueSearch(
            place.lat,
            place.lon,
            excursionCategoryChoice.toDomain()
        )
    }

    fun searchWithLocation(excursionCategoryChoice: ExcursionCategoryChoice) {
        pendingSearchRepository.queueSearchAtCurrentLocation(excursionCategoryChoice.toDomain())
    }

    private fun ExcursionCategoryChoice.toDomain(): ExcursionCategory? {
        return when (this) {
            ExcursionCategoryChoice.All -> null
            is ExcursionCategoryChoice.Single -> choice
        }
    }
}