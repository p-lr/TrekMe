package com.peterlaurence.trekme.features.shop.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.repositories.offers.extended.ExtendedOfferRepository
import com.peterlaurence.trekme.events.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExtendedOfferViewModel @Inject constructor(
    private val extendedOfferRepository: ExtendedOfferRepository,
    private val appEventBus: AppEventBus
): ViewModel() {
    val purchaseFlow = extendedOfferRepository.purchaseFlow
    val monthlySubscriptionDetailsFlow = extendedOfferRepository.monthlySubDetailsFlow
    val yearlySubscriptionDetailsFlow = extendedOfferRepository.yearlySubDetailsFlow

    fun buyMonthly() {
        val billingParams = extendedOfferRepository.getMonthlySubscriptionBillingParams()
        if (billingParams != null) {
            appEventBus.startBillingFlow(billingParams)
        }
    }

    fun buyYearly() {
        val billingParams = extendedOfferRepository.getYearlySubscriptionBillingParams()
        if (billingParams != null) {
            appEventBus.startBillingFlow(billingParams)
        }
    }
}