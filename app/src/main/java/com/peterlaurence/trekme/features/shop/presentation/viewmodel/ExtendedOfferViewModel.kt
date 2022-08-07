package com.peterlaurence.trekme.features.shop.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.billing.domain.interactors.ExtendedOfferInteractor
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExtendedOfferViewModel @Inject constructor(
    extendedOfferStateOwner: ExtendedOfferStateOwner,
    private val extendedOfferInteractor: ExtendedOfferInteractor
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
}