package com.peterlaurence.trekme.core.billing.domain.interactors

import com.peterlaurence.trekme.core.billing.domain.repositories.GpsProPurchaseRepo
import javax.inject.Inject

class GpsProPurchaseInteractor @Inject constructor(
    private val gpsProPurchaseRepo: GpsProPurchaseRepo
) {
    fun buyGpsPro() {
        gpsProPurchaseRepo.buySubscription()
    }
}