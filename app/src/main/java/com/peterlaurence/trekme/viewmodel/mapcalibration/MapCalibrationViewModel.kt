package com.peterlaurence.trekme.viewmodel.mapcalibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * View-model for the calibration view.
 *
 * @author P.Laurence on 21/11/20
 */
@HiltViewModel
class MapCalibrationViewModel @Inject constructor(
        private val mapLoader: MapLoader
) : ViewModel() {

    fun saveMap(map: Map) {
        viewModelScope.launch {
            mapLoader.saveMap(map)
        }
    }
}