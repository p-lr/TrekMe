package com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.repositories.offers.extended.ExtendedOfferRepository
import com.peterlaurence.trekme.events.AppEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class IgnLicenseViewModel @Inject constructor(
    private val repo: ExtendedOfferRepository,
    private val appEventBus: AppEventBus
) : ViewModel() {
    val purchaseStateLiveData = repo.purchaseFlow.asLiveData(viewModelScope.coroutineContext)
    val priceLiveData = repo.subDetailsFlow.map { it?.price ?: "" }.asLiveData()

    fun buyLicense() {
        val billingParams =  repo.getSubscriptionBillingParams()
        if (billingParams != null) {
            appEventBus.startBillingFlow(billingParams)
        }
    }
}
