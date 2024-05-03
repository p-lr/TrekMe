package com.peterlaurence.trekme.main.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.billing.domain.interactors.TrekmeExtendedInteractor
import com.peterlaurence.trekme.core.billing.domain.interactors.TrekmeExtendedWithIgnInteractor
import com.peterlaurence.trekme.core.billing.domain.model.GpsProStateOwner
import com.peterlaurence.trekme.core.location.domain.model.InternalGps
import com.peterlaurence.trekme.core.location.domain.model.LocationSource
import com.peterlaurence.trekme.core.map.domain.interactors.SetMapInteractor
import com.peterlaurence.trekme.core.map.domain.interactors.UpdateMapsInteractor
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.settings.StartOnPolicy
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.FatalMessage
import com.peterlaurence.trekme.events.WarningMessage
import com.peterlaurence.trekme.features.mapcreate.domain.repository.DownloadRepository
import com.peterlaurence.trekme.main.shortcut.Shortcut
import com.peterlaurence.trekme.util.android.hasLocationPermission
import com.peterlaurence.trekme.util.map
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
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
    private val trekmeExtendedWithIgnInteractor: TrekmeExtendedWithIgnInteractor,
    private val trekmeExtendedInteractor: TrekmeExtendedInteractor,
    private val gpsProStateOwner: GpsProStateOwner,
    private val appEventBus: AppEventBus,
    private val updateMapsInteractor: UpdateMapsInteractor,
    private val downloadRepository: DownloadRepository,
    private val setMapInteractor: SetMapInteractor,
    private val locationSource: LocationSource
) : ViewModel() {
    private var attemptedAtLeastOnce = false

    private val _gpsProPurchased = MutableStateFlow(false)
    val gpsProPurchased = _gpsProPurchased.asStateFlow()

    val mapsInitializing = mapRepository.mapListFlow.map {
        it == MapRepository.Loading
    }

    val downloadEvents = downloadRepository.downloadEvent

    private val event = Channel<MainActivityEvent>(1)
    val eventFlow = event.receiveAsFlow()

    /**
     * When the main activity first starts, we init the [TrekMeContext] and the [UnitFormatter].
     * The application may be started from a shortcut. In this case, the shortcut takes precedence
     * over the startup policy.
     * The startup policy has two cases:
     * * show the last viewed map
     * * show the map list
     *
     * By design, the user can't navigate until this initialization step is done. This is done by:
     * - disabling gestures on the drawer
     * - hiding the top app bar of the default destination (map list), because this bar shows a
     *   button to expand the menu.
     */
    fun onActivityStart(shortcut: Shortcut? = null) {
        viewModelScope.launch {
            if (attemptedAtLeastOnce) return@launch
            attemptedAtLeastOnce = true // remember that we tried once

            /* Prefetch location now - useful to reduce wait time */
            launch {
                if (hasLocationPermission(app.applicationContext)) {
                    locationSource.locationFlow.first()
                }
            }

            initTrekMeContext()

            mapRepository.mapsLoading()
            trekMeContext.rootDirListFlow.value.also { dirList ->
                updateMapsInteractor.updateMaps(dirList)
            }

            /* Get user-pref about metric or imperial system */
            UnitFormatter.system = settings.getMeasurementSystem().first()

            /* The shortcut takes precedence over the startup policy */
            if (shortcut != null) {
                when(shortcut) {
                    Shortcut.RECORDINGS -> event.send(MainActivityEvent.ShowRecordings)
                    Shortcut.LAST_MAP -> showLastMap()
                }
            } else {
                when (settings.getStartOnPolicy().first()) {
                    StartOnPolicy.MAP_LIST -> event.send(MainActivityEvent.ShowMapList)
                    StartOnPolicy.LAST_MAP -> showLastMap()
                }
            }
        }

        viewModelScope.launch {
            gpsProStateOwner.purchaseFlow.collect { state ->
                when (state) {
                    PurchaseState.PURCHASED -> {
                        _gpsProPurchased.value = true
                    }
                    PurchaseState.NOT_PURCHASED -> {
                        /* If denied, switch back to internal GPS */
                        settings.setLocationProducerInfo(InternalGps)
                        _gpsProPurchased.value = false
                    }
                    else -> { /* Nothing to do */
                    }
                }
            }
        }
    }

    fun onActivityResume() {
        trekmeExtendedWithIgnInteractor.acknowledgePurchase()
        trekmeExtendedInteractor.acknowledgePurchase()
    }

    fun onGoToMap(uuid: UUID) = viewModelScope.launch {
        setMapInteractor.setMap(uuid)
        event.send(MainActivityEvent.ShowMap)
    }

    private suspend fun showLastMap() {
        val id = settings.getLastMapId().firstOrNull()
        val found = id?.let {
            val map = mapRepository.getMap(id)
            map?.let {
                mapRepository.setCurrentMap(map)
                event.send(MainActivityEvent.ShowMap)
                true
            } ?: false
        } ?: false

        if (!found) {
            /* Fall back to show the map list */
            event.send(MainActivityEvent.ShowMapList)
        }
    }

    private suspend fun initTrekMeContext() {
        val ctx = app.applicationContext
        val errorTitle = ctx.getString(R.string.error_title)

        val initOk = trekMeContext.init(app.applicationContext)
        if (initOk) {
            warnIfBadStorageState()
        } else {
            appEventBus.postMessage(
                FatalMessage(
                    errorTitle,
                    ctx.getString(R.string.init_error)
                )
            )
        }
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

sealed interface MainActivityEvent {
    data object ShowMapList : MainActivityEvent
    data object ShowMap : MainActivityEvent
    data object ShowRecordings : MainActivityEvent
}
