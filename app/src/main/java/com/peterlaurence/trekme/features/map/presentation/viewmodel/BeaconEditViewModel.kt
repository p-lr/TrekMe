package com.peterlaurence.trekme.features.map.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.peterlaurence.trekme.core.map.domain.models.Beacon
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.units.DistanceUnit
import com.peterlaurence.trekme.features.map.domain.interactors.BeaconInteractor
import com.peterlaurence.trekme.features.map.presentation.ui.navigation.BeaconEditScreenArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BeaconEditViewModel @Inject constructor(
    private val beaconInteractor: BeaconInteractor,
    private val settings: Settings,
    mapRepository: MapRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = savedStateHandle.toRoute<BeaconEditScreenArgs>()
    private val mapId = UUID.fromString(args.mapId)
    private val map = mapRepository.getMap(mapId)

    private val _beaconState = MutableStateFlow<BeaconState>(Loading)
    val beaconState = _beaconState.asStateFlow()

    val radiusUnitFlow = settings.getUnitForBeaconRadius()

    init {
        val beacon = getBeacon()
        if (beacon != null) {
            _beaconState.value = Ready(beacon)
        }
    }

    fun setBeaconRadiusUnit(distanceUnit: DistanceUnit) = viewModelScope.launch {
        settings.setUnitForBeaconRadius(distanceUnit)
    }

    fun saveBeacon(lat: Double?, lon: Double?, radius: Float?, name: String, comment: String) {
        val beacon = makeBeacon(lat, lon, radius, name, comment) ?: return
        beaconInteractor.saveBeacon(mapId, beacon)
    }

    private fun makeBeacon(
        lat: Double?,
        lon: Double?,
        radius: Float?,
        name: String,
        comment: String
    ): Beacon? {
        val beacon = getBeacon() ?: return null
        return Beacon(
            args.beaconId,
            lat = lat ?: beacon.lat,
            lon = lon ?: beacon.lon,
            name = name,
            radius = radius ?: beacon.radius,
            comment = comment
        )
    }

    private fun getBeacon(): Beacon? {
        return map?.let { map ->
            map.beacons.value.firstOrNull {
                it.id == args.beaconId
            }
        }
    }

    sealed interface BeaconState
    object Loading : BeaconState
    data class Ready(val beacon: Beacon) : BeaconState
}