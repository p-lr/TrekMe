package com.peterlaurence.trekme.viewmodel.gpspro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.billing.common.PurchaseState
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.core.location.InternalGps
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.repositories.gpspro.GpsProPurchaseRepo
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
    val subscriptionDetailsFlow = repo.subDetailsFlow

    init {
        purchaseFlow.map { state ->
            when(state) {
                PurchaseState.PURCHASED -> {
                    /* As soon as we're granted access, navigate to the feature fragment */
                    //gpsProEvents.requestShowGpsProFragment()
                }
                PurchaseState.NOT_PURCHASED -> {
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

