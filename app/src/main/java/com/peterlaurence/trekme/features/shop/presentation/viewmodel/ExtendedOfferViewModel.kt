package com.peterlaurence.trekme.features.shop.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.billing.domain.interactors.ExtendedOfferInteractor
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import com.peterlaurence.trekme.events.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExtendedOfferViewModel @Inject constructor(
    extendedOfferStateOwner: ExtendedOfferStateOwner,
    private val extendedOfferInteractor: ExtendedOfferInteractor,
    private val appEventBus: AppEventBus
): ViewModel() {
    val purchaseFlow = extendedOfferStateOwner.purchaseFlow
    val monthlySubscriptionDetailsFlow = extendedOfferStateOwner.monthlySubDetailsFlow
    val yearlySubscriptionDetailsFlow = extendedOfferStateOwner.yearlySubDetailsFlow

    fun buyMonthly() {
        extendedOfferInteractor.buyMonthlySubscription()
    }

    fun buyYearly() {
        extendedOfferInteractor.buyYearlySubscription()
    }

    fun onMainMenuClick() {
        appEventBus.openDrawer()
    }
}