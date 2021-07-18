package com.peterlaurence.trekme.viewmodel.mapview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.billing.AccessDeniedLicenseOutdated
import com.peterlaurence.trekme.billing.AccessGranted
import com.peterlaurence.trekme.billing.Billing
import com.peterlaurence.trekme.billing.GracePeriod
import com.peterlaurence.trekme.billing.common.AnnualWithGracePeriodVerifier
import com.peterlaurence.trekme.billing.ign.LicenseInfo
import com.peterlaurence.trekme.billing.ign.PersistenceStrategy
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.di.IGN
import com.peterlaurence.trekme.repositories.map.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The view model of the fragment which displays [Map]s.
 *
 * @author P.Laurence on 24/08/2019
 */
@HiltViewModel
class MapViewViewModel @Inject constructor(
        private val persistenceStrategy: PersistenceStrategy,
        private val settings: Settings,
        @IGN private val billing: Billing,
        private val mapRepository: MapRepository
) : ViewModel() {
    private val _ignLicenseEvents = MutableSharedFlow<IgnLicenseEvent>(0, 1, BufferOverflow.DROP_OLDEST)
    val ignLicenseEvent = _ignLicenseEvents.asSharedFlow()

    /**
     * @return a [Map] instance, or null if there is none or there's a license issue
     */
    fun getMap(): Map? {
        val map = mapRepository.getCurrentMap()
        if (map != null) {
            viewModelScope.launch {
                checkForIgnLicense(map)
            }
        }
        return map
    }

    fun getMagnifyingFactor(): Flow<Int> = settings.getMagnifyingFactor()

    fun getMaxScale(): Flow<Float> = settings.getMaxScale()

    fun getRotationMode(): Flow<RotationMode> = settings.getRotationMode()

    fun getDefineScaleCentered(): Flow<Boolean> = settings.getDefineScaleCentered()

    fun getScaleCentered(): Flow<Float> {
        return settings.getScaleRatioCentered().combine(getMaxScale()) { scaleRatio, maxScale ->
            scaleRatio * maxScale / 100f
        }
    }

    fun getSpeedVisibility(): Flow<Boolean> = settings.getSpeedVisibility()

    fun setSpeedVisibility(v: Boolean) = viewModelScope.launch {
        settings.setSpeedVisibility(v)
    }

    fun getGpsDataVisibility(): Flow<Boolean> = settings.getGpsDataVisibility()

    suspend fun toggleGpsDataVisibility(): Boolean {
        val v = !getGpsDataVisibility().first()
        settings.setGpsDataVisibility(v)
        return v
    }

    private suspend fun checkForIgnLicense(map: Map): Boolean {
        if (map.origin != Map.MapOrigin.IGN_LICENSED) return true

        /**
         * If the persistence file doesn't exists and the license is proven to be purchased
         * and still valid, create the persistence file.
         * Otherwise, warn the user that the license is either missing or expired.
         */
        suspend fun onFailureToReadFile(): Boolean {
            // missing license or something else wrong
            return billing.getPurchase()?.let {
                persistenceStrategy.persist(LicenseInfo(it.purchaseTime))
                true
            } ?: run {
                _ignLicenseEvents.tryEmit(ErrorIgnLicenseEvent(map))
                false
            }
        }

        return withContext(Dispatchers.IO) {
            persistenceStrategy.getLicenseInfo()?.let {
                val verifier = AnnualWithGracePeriodVerifier()
                when (val accessState = verifier.checkTime(it.purchaseTimeMillis)) {
                    is AccessGranted -> true
                    is GracePeriod -> {
                        _ignLicenseEvents.tryEmit(GracePeriodIgnEvent(map, accessState.remainingDays))
                        true
                    }
                    is AccessDeniedLicenseOutdated -> {
                        _ignLicenseEvents.tryEmit(OutdatedIgnLicenseEvent(map))
                        false
                    }
                }
            } ?: onFailureToReadFile()
        }
    }
}

sealed class IgnLicenseEvent
data class OutdatedIgnLicenseEvent(val map: Map) : IgnLicenseEvent()
data class ErrorIgnLicenseEvent(val map: Map) : IgnLicenseEvent()
data class GracePeriodIgnEvent(val map: Map, val remainingDays: Int) : IgnLicenseEvent()

