package com.peterlaurence.trekme.viewmodel.mapview

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.billing.ign.*
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.model.map.MapModel
import org.greenrobot.eventbus.EventBus

/**
 * The view model of the fragment which displays [Map]s.
 *
 * @author peterLaurence on 24/08/2019
 */
class MapViewViewModel : ViewModel() {
    private val persistenceStrategy = PersistenceStrategy()

    private val eventBus = EventBus.getDefault()

    /**
     * @return a [Map] instance, or null if there is none or there's a license issue
     */
    fun getMap(): Map? {
        val map = MapModel.getCurrentMap()
        if (map != null) {
            if (checkForIgnLicense(map)) {
                return map
            }
        }
        return null
    }

    private fun checkForIgnLicense(map: Map): Boolean {
        if (map.origin != Map.MapOrigin.IGN_LICENSED) return true

        return persistenceStrategy.getLicenseInfo()?.let {
            when (val accessState = checkTime(it.purchaseTimeMillis)) {
                is AccessGranted -> true
                is GracePeriod -> {
                    eventBus.post(GracePeriodIgnEvent(map, accessState.remainingDays))
                    true
                }
                is AccessDeniedLicenseOutdated -> {
                    eventBus.post(OutdatedIgnLicenseEvent(map))
                    false
                }
            }
        } ?: {
            // missing license or something else wrong
            eventBus.post(ErrorIgnLicenseEvent(map))
            false
        }()
    }
}

data class OutdatedIgnLicenseEvent(val map: Map)
data class ErrorIgnLicenseEvent(val map: Map)
data class GracePeriodIgnEvent(val map: Map, val remainingDays: Int)