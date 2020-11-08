package com.peterlaurence.trekme.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.MainActivity
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.settings.StartOnPolicy
import com.peterlaurence.trekme.repositories.map.MapRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


/**
 * This view-model is attached to the [MainActivity].
 * It manages model specific considerations that are set here outside of the main activity which
 * should mainly used to manage fragments.
 *
 * Avoids excessive [MapLoader.updateMaps] calls by managing an internal [attemptedAtLeastOnce] flag.
 * It is also important because the activity might start after an Intent with a result code. In this
 * case, [attemptedAtLeastOnce] is true and we shall not trigger background processing.
 *
 * @author P.Laurence on 07/10/2019
 */
class MainActivityViewModel @ViewModelInject constructor(
        private val trekMeContext: TrekMeContext,
        private val settings: Settings,
        private val mapRepository: MapRepository
) : ViewModel() {
    private var attemptedAtLeastOnce = false

    private val _showMapListSignal = MutableSharedFlow<Unit>()
    val showMapListSignal = _showMapListSignal.asSharedFlow()

    private val _showMapViewSignal = MutableSharedFlow<Unit>()
    val showMapViewSignal = _showMapViewSignal.asSharedFlow()

    /**
     * When the [MainActivity] first starts, we either:
     * * show the last viewed map
     * * show the map list
     */
    fun onActivityStart() {
        viewModelScope.launch {
            if (attemptedAtLeastOnce) return@launch
            attemptedAtLeastOnce = true // remember that we tried once

            MapLoader.clearMaps()
            trekMeContext.mapsDirList?.also { dirList ->
                MapLoader.updateMaps(dirList)
            }

            when (settings.getStartOnPolicy()) {
                StartOnPolicy.MAP_LIST -> _showMapListSignal.emit(Unit)
                StartOnPolicy.LAST_MAP -> {
                    val id = settings.getLastMapId()
                    val found = id?.let {
                        val map = MapLoader.getMap(id)
                        map?.let {
                            mapRepository.setCurrentMap(map)
                            _showMapViewSignal.emit(Unit)
                            true
                        } ?: false
                    } ?: false

                    if (!found) {
                        /* Fall back to show the map list */
                        _showMapListSignal.emit(Unit)
                    }
                }
            }
        }
    }

    fun getMapIndex(mapId: Int): Int = MapLoader.maps.indexOfFirst { it.id == mapId }
}
