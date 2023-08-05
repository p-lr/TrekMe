package com.peterlaurence.trekme.features.shop.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.billing.di.IGN
import com.peterlaurence.trekme.core.billing.domain.interactors.TrekmeExtendedWithIgnInteractor
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExtendedWithIgnViewModel @Inject constructor(
    @IGN
    extendedOfferWithIgnStateOwner: ExtendedOfferStateOwner,
    private val trekmeExtendedWithIgnInteractor: TrekmeExtendedWithIgnInteractor,
): ViewModel() {
    val purchaseFlow = extendedOfferWithIgnStateOwner.purchaseFlow
    val monthlySubscriptionDetailsFlow = extendedOfferWithIgnStateOwner.monthlySubDetailsFlow
    val yearlySubscriptionDetailsFlow = extendedOfferWithIgnStateOwner.yearlySubDetailsFlow

    fun buyMonthly() {
        trekmeExtendedWithIgnInteractor.buyMonthlySubscription()
    }

    fun buyYearly() {
        trekmeExtendedWithIgnInteractor.buyYearlySubscription()
    }
}