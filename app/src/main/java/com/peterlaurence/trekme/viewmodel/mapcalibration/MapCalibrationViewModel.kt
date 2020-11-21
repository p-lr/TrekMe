package com.peterlaurence.trekme.viewmodel.mapcalibration

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import kotlinx.coroutines.launch

/**
 * View-model for the calibration view.
 *
 * @author P.Laurence on 21/11/20
 */
class MapCalibrationViewModel @ViewModelInject constructor(
        private val mapLoader: MapLoader
): ViewModel() {

    fun saveMap(map: Map) {
        viewModelScope.launch {
            mapLoader.saveMap(map)
        }
    }
}