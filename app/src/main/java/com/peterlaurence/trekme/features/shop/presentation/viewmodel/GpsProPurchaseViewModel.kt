package com.peterlaurence.trekme.features.shop.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.repositories.gpspro.GpsProPurchaseRepo
import com.peterlaurence.trekme.events.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GpsProPurchaseViewModel @Inject constructor(
    private val repo: GpsProPurchaseRepo,
    private val appEventBus: AppEventBus,
) : ViewModel() {
    val purchaseFlow = repo.purchaseFlow
    val subscriptionDetailsFlow = repo.subDetailsFlow

    fun buy() {
        val billingParams = repo.getSubscriptionBillingParams()
        if (billingParams != null) {
            appEventBus.startBillingFlow(billingParams)
        }
    }
}

