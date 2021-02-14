package com.peterlaurence.trekme.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.MainActivity
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.events.WarningMessage
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.settings.StartOnPolicy
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.repositories.map.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


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
@HiltViewModel
class MainActivityViewModel @Inject constructor(
        private val app: Application,
        private val trekMeContext: TrekMeContext,
        private val settings: Settings,
        private val mapRepository: MapRepository,
        private val appEventBus: AppEventBus,
        private val mapLoader: MapLoader
) : ViewModel() {
    private var attemptedAtLeastOnce = false

    private val _showMapListSignal = MutableSharedFlow<Unit>()
    val showMapListSignal = _showMapListSignal.asSharedFlow()

    private val _showMapViewSignal = MutableSharedFlow<Unit>()
    val showMapViewSignal = _showMapViewSignal.asSharedFlow()

    /**
     * When the [MainActivity] first starts, we init the [TrekMeContext] and the [UnitFormatter].
     * Then, we either:
     * * show the last viewed map
     * * show the map list
     */
    fun onActivityStart() {
        viewModelScope.launch {
            if (attemptedAtLeastOnce) return@launch
            attemptedAtLeastOnce = true // remember that we tried once

            trekMeContext.init(app.applicationContext)
            warnIfBadStorageState()

            mapLoader.clearMaps()
            trekMeContext.mapsDirList?.also { dirList ->
                mapLoader.updateMaps(dirList)
            }

            /* Get user-pref about metric or imperial system */
            UnitFormatter.system = settings.getMeasurementSystem()

            when (settings.getStartOnPolicy()) {
                StartOnPolicy.MAP_LIST -> _showMapListSignal.emit(Unit)
                StartOnPolicy.LAST_MAP -> {
                    val id = settings.getLastMapId()
                    val found = id?.let {
                        val map = mapLoader.getMap(id)
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

    private suspend fun warnIfBadStorageState() {
        /* If something is wrong.. */
        if (!trekMeContext.checkAppDir()) {
            val ctx = app.applicationContext
            val warningTitle = ctx.getString(R.string.warning_title)
            if (trekMeContext.isAppDirReadOnly()) {
                /* If its read only for sure, be explicit */
                appEventBus.postMessage(WarningMessage(warningTitle, ctx.getString(R.string.storage_read_only)))
            } else {
                /* Else, just say there is something wrong */
                appEventBus.postMessage(WarningMessage(warningTitle, ctx.getString(R.string.bad_storage_status)))
            }
        }
    }

    fun getMapIndex(mapId: Int): Int = mapLoader.maps.indexOfFirst { it.id == mapId }
}
