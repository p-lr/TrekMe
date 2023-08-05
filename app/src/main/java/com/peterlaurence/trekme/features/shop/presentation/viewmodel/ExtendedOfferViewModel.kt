package com.peterlaurence.trekme.features.shop.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.billing.di.TrekmeExtended
import com.peterlaurence.trekme.core.billing.domain.interactors.TrekmeExtendedInteractor
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExtendedOfferViewModel @Inject constructor(
    @TrekmeExtended
    extendedOfferStateOwner: ExtendedOfferStateOwner,
    private val trekmeExtendedInteractor: TrekmeExtendedInteractor,
) : ViewModel() {
    val purchaseFlow = extendedOfferStateOwner.purchaseFlow
    val monthlySubscriptionDetailsFlow = extendedOfferStateOwner.monthlySubDetailsFlow
    val yearlySubscriptionDetailsFlow = extendedOfferStateOwner.yearlySubDetailsFlow

    fun buyMonthly() {
        trekmeExtendedInteractor.buyMonthlySubscription()
    }

    fun buyYearly() {
        trekmeExtendedInteractor.buyYearlySubscription()
    }
}