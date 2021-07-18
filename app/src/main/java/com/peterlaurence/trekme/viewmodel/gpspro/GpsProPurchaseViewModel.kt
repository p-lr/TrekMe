package com.peterlaurence.trekme.viewmodel.gpspro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.model.InternalGps
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.repositories.gpspro.GpsProPurchaseRepo
import com.peterlaurence.trekme.repositories.gpspro.GpsProPurchaseState
import com.peterlaurence.trekme.ui.gpspro.events.GpsProEvents
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class GpsProPurchaseViewModel @Inject constructor(
        private val repo: GpsProPurchaseRepo,
        private val appEventBus: AppEventBus,
        private val gpsProEvents: GpsProEvents,
        private val settings: Settings
) : ViewModel() {
    val purchaseFlow = repo.purchaseFlow
    val priceFlow = repo.subDetailsFlow.map { it?.price }

    init {
        purchaseFlow.map { state ->
            when(state) {
                GpsProPurchaseState.ACCESS_GRANTED -> {
                    /* As soon as we're granted access, navigate to the feature fragment */
                    gpsProEvents.requestShowGpsProFragment()
                }
                GpsProPurchaseState.ACCESS_DENIED -> {
                    /* If denied, switch back to internal GPS */
                    settings.setLocationProducerInfo(InternalGps)
                }
                else -> { /* Nothing to do */ }
            }
        }.launchIn(viewModelScope)
    }

    fun buy() {
        val billingParams = repo.buySubscription()
        if (billingParams != null) {
            appEventBus.startBillingFlow(billingParams)
        }
    }
}

