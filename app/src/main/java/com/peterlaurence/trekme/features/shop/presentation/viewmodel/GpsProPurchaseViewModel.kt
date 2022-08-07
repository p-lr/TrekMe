package com.peterlaurence.trekme.features.shop.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.billing.domain.model.GpsProStateOwner
import com.peterlaurence.trekme.core.billing.domain.interactors.GpsProPurchaseInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class GpsProPurchaseViewModel @Inject constructor(
    gpsProStateOwner: GpsProStateOwner,
    private val gpsProPurchaseInteractor: GpsProPurchaseInteractor,
) : ViewModel() {
    val purchaseFlow = gpsProStateOwner.purchaseFlow
    val subscriptionDetailsFlow = gpsProStateOwner.subDetailsFlow

    fun buy() {
        gpsProPurchaseInteractor.buyGpsPro()
    }
}

