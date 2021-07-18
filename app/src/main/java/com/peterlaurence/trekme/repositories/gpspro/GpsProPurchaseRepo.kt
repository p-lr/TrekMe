package com.peterlaurence.trekme.repositories.gpspro

import android.util.Log
import com.peterlaurence.trekme.billing.Billing
import com.peterlaurence.trekme.billing.BillingParams
import com.peterlaurence.trekme.billing.SubscriptionDetails
import com.peterlaurence.trekme.di.GpsPro
import com.peterlaurence.trekme.viewmodel.mapcreate.NotSupportedException
import com.peterlaurence.trekme.viewmodel.mapcreate.ProductNotFoundException
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
        mainDispatcher: CoroutineDispatcher,
        @GpsPro private val billing: Billing
) {
    private val scope = CoroutineScope(mainDispatcher + SupervisorJob())

    private val _purchaseFlow = MutableStateFlow(GpsProPurchaseState.CHECKING)
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
                    GpsProPurchaseState.ACCESS_GRANTED
                } else {
                    getSubscriptionInfo()
                    GpsProPurchaseState.ACCESS_DENIED
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
                _purchaseFlow.value = GpsProPurchaseState.ACCESS_GRANTED
            } catch (e: IllegalStateException) {
                // Can't check license info
                Log.e(TAG, e.message ?: Log.getStackTraceString(e))
                _purchaseFlow.value = GpsProPurchaseState.UNKNOWN
            } catch (e: NotSupportedException) {
                // TODO: alert the user that it can't buy the license and should ask for refund
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
        _purchaseFlow.value = GpsProPurchaseState.PURCHASE_PENDING
    }

    private fun onPurchaseAcknowledged() {
        _purchaseFlow.value = GpsProPurchaseState.ACCESS_GRANTED
    }
}

// TODO: remove this type and use a common type with LicenseStatus (put it in core.billing.common)
// and call it PurchaseState
enum class GpsProPurchaseState {
    CHECKING, ACCESS_GRANTED, ACCESS_DENIED, PURCHASE_PENDING, UNKNOWN
}

private const val TAG = "GpsProPurchaseRepo"