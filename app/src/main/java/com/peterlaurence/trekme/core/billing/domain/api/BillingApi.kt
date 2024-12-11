package com.peterlaurence.trekme.core.billing.domain.api

import com.peterlaurence.trekme.core.billing.domain.model.SubscriptionDetails
import kotlinx.coroutines.flow.Flow
import java.util.*

interface BillingApi {
    val purchaseAcknowledgedEvent: Flow<Unit>
    suspend fun isPurchased(): Boolean
    suspend fun acknowledgePurchase(): Boolean
    suspend fun getSubDetails(index: Int = 0): SubscriptionDetails?
    fun launchBilling(id: UUID, purchasePendingCb: () -> Unit)
}