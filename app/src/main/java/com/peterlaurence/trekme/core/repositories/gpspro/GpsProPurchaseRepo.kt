package com.peterlaurence.trekme.core.repositories.gpspro

import android.util.Log
import com.peterlaurence.trekme.billing.*
import com.peterlaurence.trekme.billing.common.PurchaseState
import com.peterlaurence.trekme.di.GpsPro
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
        @GpsPro private val billing: Billing
) {
    private val scope = CoroutineScope(mainDispatcher + SupervisorJob())

    private val _purchaseFlow = MutableStateFlow(PurchaseState.CHECK_PENDING)
    val purchaseFlow = _purchaseFlow.asStateFlow()

    private val _subDetailsFlow = MutableStateFlow<SubscriptionDetails?>(null)
    val subDetailsFlow = _subDetailsFlow.asStateFlow()

    init {
        scope.launch {

            /* Check if we just need to acknowledge the purchase */
            val ackDone = billing.acknowledgePurchase(this@GpsProPurchaseRepo::onPurchaseAcknowledged)

            /* Otherwise, do normal checks */
            if (!ackDone) {
                val p = billing.getPurchase()
                val result = if (p != null) {
                    PurchaseState.PURCHASED
                } else {
                    getSubscriptionInfo()
                    PurchaseState.NOT_PURCHASED
                }
                _purchaseFlow.value = result
            }
        }
    }

    private fun getSubscriptionInfo() {
        scope.launch {
            try {
                val subDetails = billing.getSubDetails()
                _subDetailsFlow.value = subDetails
            } catch (e: ProductNotFoundException) {
                // Something wrong on our side
                _purchaseFlow.value = PurchaseState.PURCHASED
            } catch (e: IllegalStateException) {
                // Can't check license info
                Log.e(TAG, e.message ?: Log.getStackTraceString(e))
                _purchaseFlow.value = PurchaseState.UNKNOWN
            } catch (e: NotSupportedException) {
                // TODO: alert the user that it can't buy the module
            }
        }
    }

    fun buySubscription(): BillingParams? {
        val ignLicenseDetails = _subDetailsFlow.value
        return if (ignLicenseDetails != null) {
            billing.launchBilling(ignLicenseDetails.skuDetails, this::onPurchaseAcknowledged, this::onPurchasePending)
        } else null
    }

    private fun onPurchasePending() {
        _purchaseFlow.value = PurchaseState.PURCHASE_PENDING
    }

    private fun onPurchaseAcknowledged() {
        _purchaseFlow.value = PurchaseState.PURCHASED
    }
}

private const val TAG = "GpsProPurchaseRepo"