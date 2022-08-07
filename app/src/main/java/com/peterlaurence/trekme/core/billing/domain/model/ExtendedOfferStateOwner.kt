package com.peterlaurence.trekme.core.billing.domain.model

import kotlinx.coroutines.flow.StateFlow

interface ExtendedOfferStateOwner {
    val purchaseFlow: StateFlow<PurchaseState>
    val yearlySubDetailsFlow: StateFlow<SubscriptionDetails?>
    val monthlySubDetailsFlow: StateFlow<SubscriptionDetails?>
}