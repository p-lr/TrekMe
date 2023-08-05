package com.peterlaurence.trekme.core.billing.domain.interactors

import com.peterlaurence.trekme.core.billing.domain.repositories.TrekmeExtendedWithIgnRepository
import javax.inject.Inject

class TrekmeExtendedWithIgnInteractor @Inject constructor(
    private val repository: TrekmeExtendedWithIgnRepository
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