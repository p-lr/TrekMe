package com.peterlaurence.trekme.viewmodel.mapview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.billing.AccessDeniedLicenseOutdated
import com.peterlaurence.trekme.billing.AccessGranted
import com.peterlaurence.trekme.billing.Billing
import com.peterlaurence.trekme.billing.GracePeriod
import com.peterlaurence.trekme.billing.common.AnnualWithGracePeriodVerifier
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.settings.RotationMode
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.di.IGN
import com.peterlaurence.trekme.repositories.map.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
        private val settings: Settings,
        @IGN private val billing: Billing,
        private val mapRepository: MapRepository
) : ViewModel() {

    fun getMap(): Map? = mapRepository.getCurrentMap()

    /**
     * Get the license specific to the current map at the moment of this call. As a side note: in
     * this case, a cold flow is preferable to a shared flow as we want to make sure that the
     * returned flow is specific to the caller.
     */
    fun getLicenseFlow(): Flow<LicenseEvent> {
        return flow {
            val map = mapRepository.getCurrentMap()
            if (map != null) {
                emit(getLicense(map))
            }
        }
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

    fun getOrientationVisibility(): Flow<Boolean> = settings.getOrientationVisibility()

    fun setOrientationVisibility(v: Boolean) = viewModelScope.launch {
        settings.setOrientationVisibility(v)
    }

    fun getGpsDataVisibility(): Flow<Boolean> = settings.getGpsDataVisibility()

    suspend fun toggleGpsDataVisibility(): Boolean {
        val v = !getGpsDataVisibility().first()
        settings.setGpsDataVisibility(v)
        return v
    }

    private suspend fun getLicense(map: Map): LicenseEvent {
        if (map.origin != Map.MapOrigin.IGN_LICENSED) return FreeLicense

        return withContext(Dispatchers.IO) {
            billing.getPurchase()?.let {
                val verifier = AnnualWithGracePeriodVerifier()
                when (val accessState = verifier.checkTime(it.purchaseTime)) {
                    is AccessGranted -> ValidIgnLicense
                    is GracePeriod -> {
                       GracePeriodIgnEvent(map, accessState.remainingDays)
                    }
                    is AccessDeniedLicenseOutdated -> {
                        OutdatedIgnLicenseEvent(map)
                    }
                }
            } ?: ErrorIgnLicenseEvent(map)
        }
    }
}

sealed interface LicenseEvent
object FreeLicense : LicenseEvent
object ValidIgnLicense : LicenseEvent
data class OutdatedIgnLicenseEvent(val map: Map) : LicenseEvent
data class ErrorIgnLicenseEvent(val map: Map) : LicenseEvent
data class GracePeriodIgnEvent(val map: Map, val remainingDays: Int) : LicenseEvent

