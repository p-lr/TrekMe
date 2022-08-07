package com.peterlaurence.trekme.core.billing.domain.interactors

import com.peterlaurence.trekme.core.billing.domain.repositories.ExtendedOfferRepository
import javax.inject.Inject

class ExtendedOfferInteractor @Inject constructor(
    private val repository: ExtendedOfferRepository
) {
    fun buyMonthlySubscription() {
        repository.buyMonthlySubscription()
    }

    fun buyYearlySubscription() {
        repository.buyYearlySubscription()
    }

    fun acknowledgePurchase() {
        repository.acknowledgePurchase()
    }
}