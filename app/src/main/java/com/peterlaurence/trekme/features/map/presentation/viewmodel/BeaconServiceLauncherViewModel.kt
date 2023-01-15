package com.peterlaurence.trekme.features.map.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class BeaconServiceLauncherViewModel @Inject constructor(
    mapFeatureEvents: MapFeatureEvents
): ViewModel() {
    val backgroundLocationRequest: Flow<Unit> = mapFeatureEvents.hasBeaconsFlow
}