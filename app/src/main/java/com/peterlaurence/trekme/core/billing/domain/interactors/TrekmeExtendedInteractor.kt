package com.peterlaurence.trekme.core.billing.domain.interactors

import com.peterlaurence.trekme.core.billing.domain.repositories.TrekmeExtendedRepository
import javax.inject.Inject

class TrekmeExtendedInteractor @Inject constructor(
    private val repository: TrekmeExtendedRepository
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