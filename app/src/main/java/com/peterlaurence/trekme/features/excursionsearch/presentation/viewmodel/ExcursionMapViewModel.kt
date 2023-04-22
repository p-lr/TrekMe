package com.peterlaurence.trekme.features.excursionsearch.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ExcursionMapViewModel @Inject constructor(
    locationSource: LocationSource
): ViewModel() {
    val locationFlow: Flow<Location> = locationSource.locationFlow

    fun onLocation(location: Location) {
        println("xxxxxx on location $location")
    }
}