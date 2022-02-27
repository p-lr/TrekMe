package com.peterlaurence.trekme.core.repositories.offers.extended

import com.peterlaurence.trekme.billing.Billing
import com.peterlaurence.trekme.billing.BillingParams
import com.peterlaurence.trekme.billing.SubscriptionDetails
import com.peterlaurence.trekme.billing.common.PurchaseState
import com.peterlaurence.trekme.di.IGN
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
class ExtendedOfferRepository @Inject constructor(
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
    @IGN private val billing: Billing
) {
    private val scope = CoroutineScope(mainDispatcher + SupervisorJob())

    private val _purchaseFlow = MutableStateFlow(PurchaseState.CHECK_PENDING)
    val purchaseFlow = _purchaseFlow.asStateFlow()

    private val _subDetailsFlow = MutableStateFlow<SubscriptionDetails?>(null)
    val subDetailsFlow = _subDetailsFlow.asStateFlow()

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
                val p = billing.getPurchase()
                val result = if (p != null) {
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

    fun acknowledgePurchase() = scope.launch {
        val ackDone = billing.acknowledgePurchase()
        if (ackDone) {
            onPurchaseAcknowledged()
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

    fun getSubscriptionBillingParams(): BillingParams? {
        val subscriptionDetails = _subDetailsFlow.value
        return if (subscriptionDetails != null) {
            billing.launchBilling(subscriptionDetails.skuDetails, this::onPurchasePending)
        } else null
    }

    private fun onPurchasePending() {
        _purchaseFlow.value = PurchaseState.PURCHASE_PENDING
    }

    private fun onPurchaseAcknowledged() {
        _purchaseFlow.value = PurchaseState.PURCHASED
    }
}