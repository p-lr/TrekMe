package com.peterlaurence.trekme.core.billing.domain.repositories

import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.billing.di.GpsPro
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.core.billing.domain.model.GpsProStateOwner
import com.peterlaurence.trekme.core.billing.domain.model.SubscriptionDetails
import com.peterlaurence.trekme.di.MainDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpsProPurchaseRepo @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    @GpsPro private val billing: BillingApi
) : GpsProStateOwner {
    private val scope = CoroutineScope(mainDispatcher + SupervisorJob())

    private val _purchaseFlow = MutableStateFlow(PurchaseState.CHECK_PENDING)
    override val purchaseFlow = _purchaseFlow.asStateFlow()

    private val _subDetailsFlow = MutableStateFlow<SubscriptionDetails?>(null)
    override val subDetailsFlow = _subDetailsFlow.asStateFlow()

    init {
        scope.launch {
            billing.purchaseAcknowledgedEvent.collect {
                onPurchaseAcknowledged()
            }
        }

        scope.launch {

            /* Check if we just need to acknowledge the purchase */
            val ackDone = billing.acknowledgePurchase()

            /* Otherwise, do normal checks */
            if (!ackDone) {
                val result = if (billing.isPurchased()) {
                    PurchaseState.PURCHASED
                } else {
                    updateSubscriptionInfo()
                    PurchaseState.NOT_PURCHASED
                }
                _purchaseFlow.value = result
            } else {
                onPurchaseAcknowledged()
            }
        }
    }

    private fun updateSubscriptionInfo() {
        scope.launch {
            runCatching {
                val subDetails = billing.getSubDetails()
                _subDetailsFlow.value = subDetails
            }
        }
    }

    fun buySubscription() {
        val ignLicenseDetails = _subDetailsFlow.value
        if (ignLicenseDetails != null) {
            billing.launchBilling(ignLicenseDetails.id, this::onPurchasePending)
        }
    }

    private fun onPurchasePending() {
        _purchaseFlow.value = PurchaseState.PURCHASE_PENDING
    }

    private fun onPurchaseAcknowledged() {
        _purchaseFlow.value = PurchaseState.PURCHASED
    }
}
