package com.peterlaurence.trekme.main

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.billing.domain.interactors.ExtendedOfferInteractor
import com.peterlaurence.trekme.core.billing.domain.model.GpsProStateOwner
import com.peterlaurence.trekme.core.location.InternalGps
import com.peterlaurence.trekme.core.map.domain.interactors.UpdateMapsInteractor
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.settings.StartOnPolicy
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.WarningMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject


/**
 * This view-model is attached to the [MainActivity].
 * It manages model specific considerations that are set here outside of the main activity which
 * should mainly used to manage fragments.
 *
 * Avoids excessive [UpdateMapsInteractor.updateMaps] calls by managing an internal [attemptedAtLeastOnce] flag.
 * It is also important because the activity might start after an Intent with a result code. In this
 * case, [attemptedAtLeastOnce] is true and we shall not trigger background processing.
 *
 * @since 2019/10/07
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val app: Application,
    private val trekMeContext: TrekMeContext,
    private val settings: Settings,
    private val mapRepository: MapRepository,
    private val extendedOfferInteractor: ExtendedOfferInteractor,
    private val gpsProStateOwner: GpsProStateOwner,
    private val appEventBus: AppEventBus,
    private val updateMapsInteractor: UpdateMapsInteractor
) : ViewModel() {
    private var attemptedAtLeastOnce = false

    private val _showMapListSignal = MutableSharedFlow<Unit>()
    val showMapListSignal = _showMapListSignal.asSharedFlow()

    private val _showMapViewSignal = MutableSharedFlow<Unit>()
    val showMapViewSignal = _showMapViewSignal.asSharedFlow()

    val gpsProPurchased = MutableLiveData(false)

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

            mapRepository.mapsLoading()
            trekMeContext.rootDirList?.also { dirList ->
                updateMapsInteractor.updateMaps(dirList)
            }

            /* Get user-pref about metric or imperial system */
            UnitFormatter.system = settings.getMeasurementSystem().first()

            when (settings.getStartOnPolicy().first()) {
                StartOnPolicy.MAP_LIST -> _showMapListSignal.emit(Unit)
                StartOnPolicy.LAST_MAP -> {
                    val id = settings.getLastMapId().firstOrNull()
                    val found = id?.let {
                        val map = mapRepository.getMap(id)
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

        viewModelScope.launch {
            gpsProStateOwner.purchaseFlow.collect { state ->
                when (state) {
                    PurchaseState.PURCHASED -> {
                        gpsProPurchased.value = true
                    }
                    PurchaseState.NOT_PURCHASED -> {
                        /* If denied, switch back to internal GPS */
                        settings.setLocationProducerInfo(InternalGps)
                        gpsProPurchased.value = false
                    }
                    else -> { /* Nothing to do */
                    }
                }
            }
        }
    }

    fun onActivityResume() {
        extendedOfferInteractor.acknowledgePurchase()
    }

    private suspend fun warnIfBadStorageState() {
        /* If something is wrong.. */
        if (!trekMeContext.checkAppDir()) {
            val ctx = app.applicationContext
            val warningTitle = ctx.getString(R.string.warning_title)
            if (trekMeContext.isAppDirReadOnly()) {
                /* If its read only for sure, be explicit */
                appEventBus.postMessage(
                    WarningMessage(
                        warningTitle,
                        ctx.getString(R.string.storage_read_only)
                    )
                )
            } else {
                /* Else, just say there is something wrong */
                appEventBus.postMessage(
                    WarningMessage(
                        warningTitle,
                        ctx.getString(R.string.bad_storage_status)
                    )
                )
            }
        }
    }

    fun getMapIndex(mapId: UUID): Int = mapRepository.getCurrentMapList().indexOfFirst { it.id == mapId }
}
