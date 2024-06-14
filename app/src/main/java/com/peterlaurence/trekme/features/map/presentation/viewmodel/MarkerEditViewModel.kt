package com.peterlaurence.trekme.features.map.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.features.map.domain.interactors.MarkerInteractor
import com.peterlaurence.trekme.features.map.presentation.ui.navigation.MarkerEditArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MarkerEditViewModel @Inject constructor(
    mapRepository: MapRepository,
    private val markerInteractor: MarkerInteractor,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val args = MarkerEditArgs(savedStateHandle)
    private val mapId = UUID.fromString(args.mapId)
    private val map = mapRepository.getMap(mapId)

    private val _markerState = MutableStateFlow<Marker?>(null)
    val markerState = _markerState.asStateFlow()

    init {
        _markerState.value = getMarker()
    }

    fun saveMarker(lat: Double?, lon: Double?, name: String, comment: String, color: String?) {
        val marker = makeMarker(lat, lon, name, comment, color) ?: return
        markerInteractor.saveMarkerDebounced(mapId, marker)
    }

    private fun makeMarker(lat: Double?, lon: Double?, name: String, comment: String, color: String?): Marker? {
        val marker = getMarker() ?: return null
        return Marker(
            args.markerId,
            lat = lat ?: marker.lat,
            lon = lon ?: marker.lon,
            name = name,
            comment = comment,
            color = color ?: marker.color
        )
    }

    private fun getMarker(): Marker? {
        return map?.let { map ->
            map.markers.value.firstOrNull {
                it.id == args.markerId
            }
        }
    }
}