package com.peterlaurence.trekme.billing.ign

import android.app.Application
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.*
import com.peterlaurence.trekme.viewmodel.mapcreate.IgnLicenseDetails
import com.peterlaurence.trekme.viewmodel.mapcreate.NotSupportedException
import com.peterlaurence.trekme.viewmodel.mapcreate.ProductNotFoundException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val IGN_LICENSE_ONETIME_SKU = "ign_license"
const val IGN_LICENSE_SUBSCRIPTION_SKU = "ign_license_sub"
typealias PurchaseAcknowledgedCallback = () -> Unit
typealias PurchasePendingCallback = () -> Unit

/**
 * Manages the licensing for IGN maps.
 *
 * @author P.Laurence on 06/08/2019
 */
class Billing(val application: Application) : PurchasesUpdatedListener, AcknowledgePurchaseResponseListener {

    private val billingClient = BillingClient.newBuilder(application).setListener(this).enablePendingPurchases().build()

    private lateinit var purchaseAcknowledgedCallback: PurchaseAcknowledgedCallback
    private lateinit var purchasePendingCallback: PurchasePendingCallback

    private var connected = false
    private val connectionStateListener = object : BillingClientStateListener {
        override fun onBillingSetupFinished(billingResult: BillingResult) {
            connected = billingResult.responseCode == OK
        }

        override fun onBillingServiceDisconnected() {
            connected = false
        }
    }

    /**
     * Attempts to connect the billing service. This function immediately returns.
     * See also [connectWithRetry], which suspends at most 10s.
     * Don't try to make this a suspend function - the [billingClient] keeps a reference on the
     * [BillingClientStateListener] so it would keep a reference on a continuation (leading to
     * insidious memory leaks, depending on who invokes that suspending function).
     * Done this way, we're sure that the [billingClient] only has a reference on this [Billing]
     * instance.
     */
    private fun connectClient() {
        if (billingClient.isReady) {
            connected = true
            return
        }
        billingClient.startConnection(connectionStateListener)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        fun acknowledge() {
            purchases?.forEach {
                if (it.sku == IGN_LICENSE_ONETIME_SKU || it.sku == IGN_LICENSE_SUBSCRIPTION_SKU) {
                    if (it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged) {
                        acknowledgeIgnLicense(it)
                    } else if (it.purchaseState == Purchase.PurchaseState.PENDING) {
                        if (this::purchasePendingCallback.isInitialized) {
                            purchasePendingCallback()
                        }
                    }
                }
            }
        }

        if (purchases != null) {
            if (billingResult.responseCode == OK) {
                acknowledge()
            }
        }
    }

    private fun acknowledgeIgnLicense(purchase: Purchase) {
        /* Approve the payment */
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams, this)
    }

    private fun shouldAcknowledgeIgnLicense(purchase: Purchase): Boolean {
        return (purchase.sku == IGN_LICENSE_SUBSCRIPTION_SKU || purchase.sku == IGN_LICENSE_ONETIME_SKU)
                && purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged
    }

    /**
     * This is the callback of the [BillingClient.acknowledgePurchase] call.
     * The [purchaseAcknowledgedCallback] is called only if the purchase is successfully acknowledged.
     */
    override fun onAcknowledgePurchaseResponse(billingResult: BillingResult) {
        /* Then notify registered PurchaseListener that it completed normally */
        if (billingResult.responseCode == OK && this::purchaseAcknowledgedCallback.isInitialized) {
            purchaseAcknowledgedCallback()
        } else {
            Log.e(TAG, "Payment couldn't be acknowledged (code ${billingResult.responseCode}): ${billingResult.debugMessage}")
        }
    }

    /**
     * This is one of the first things to do. If the IGN license is among the purchases, check if it
     * should be acknowledged. This call is required when the acknowledgement wasn't done right after
     * a billing flow (typically when the payment method is slow and the user didn't wait the end of
     * the procedure with the [onPurchasesUpdated] call). So we can end up with a purchase which is
     * in [Purchase.PurchaseState.PURCHASED] state but not acknowledged.
     * This is why the acknowledgement is also made here.
     */
    suspend fun acknowledgeIgnLicense(purchaseAcknowledgedCallback: PurchaseAcknowledgedCallback): Boolean {
        runCatching { connectWithRetry() }.onFailure { return false }

        val inAppPurchases = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
        val oneTimeAck = inAppPurchases.purchasesList?.getIgnLicenseOneTime()?.let {
            this.purchaseAcknowledgedCallback = purchaseAcknowledgedCallback
            if (shouldAcknowledgeIgnLicense(it)) {
                acknowledgeIgnLicense(it)
                true
            } else false
        } ?: false

        val subs = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
        val subAck = subs.purchasesList?.getIgnLicenseSub()?.let {
            if (shouldAcknowledgeIgnLicense(it)) {
                acknowledgeIgnLicense(it)
                true
            } else false
        } ?: false

        return oneTimeAck || subAck
    }

    suspend fun getIgnLicensePurchase(): Purchase? {
        runCatching { connectWithRetry() }.onFailure { return null }

        val inAppPurchases = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
        val oneTimeLicense = inAppPurchases.purchasesList?.getValidIgnLicenseOneTime()?.let {
            if (checkTime(it.purchaseTime) !is AccessGranted) {
                it.consumeIgnLicense()
                null
            } else it
        }

        if (oneTimeLicense == null) {
            val subs = billingClient.queryPurchases(BillingClient.SkuType.SUBS)
            /* The user might have cancelled the subscription. However, he still has the license
             * until the end of the year (plus the grace period). */
            return subs.purchasesList?.getValidIgnLicenseSub()
        }
        return oneTimeLicense
    }

    private fun List<Purchase>.getIgnLicenseOneTime(): Purchase? {
        return firstOrNull { it.sku == IGN_LICENSE_ONETIME_SKU }
    }

    private fun List<Purchase>.getIgnLicenseSub(): Purchase? {
        return firstOrNull { it.sku == IGN_LICENSE_SUBSCRIPTION_SKU }
    }

    private fun List<Purchase>.getValidIgnLicenseOneTime(): Purchase? {
        return firstOrNull { it.sku == IGN_LICENSE_ONETIME_SKU && it.isAcknowledged }
    }

    private fun List<Purchase>.getValidIgnLicenseSub(): Purchase? {
        return firstOrNull { it.sku == IGN_LICENSE_SUBSCRIPTION_SKU && it.isAcknowledged }
    }

    private fun Purchase.consumeIgnLicense() {
        if (sku == IGN_LICENSE_ONETIME_SKU || sku == IGN_LICENSE_SUBSCRIPTION_SKU) {
            consume(purchaseToken)
        }
    }

    private fun consume(token: String) {
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(token).build()
        billingClient.consumeAsync(consumeParams) { _, _ ->
            Log.i(TAG, "Consumed the purchase. It can now be bought again.")
        }
    }

    /**
     * Get the details of the IGN annual subscription.
     */
    suspend fun getIgnLicenseDetails(): IgnLicenseDetails {
        connectWithRetry()
        val (billingResult, skuDetailsList) = queryIgnLicenseDetails()
        return when (billingResult.responseCode) {
            OK -> skuDetailsList.find { it.sku == IGN_LICENSE_SUBSCRIPTION_SKU }?.let {
                IgnLicenseDetails(it)
            } ?: throw ProductNotFoundException()
            FEATURE_NOT_SUPPORTED -> throw NotSupportedException()
            SERVICE_DISCONNECTED -> error("should retry")
            else -> error("other error")
        }
    }

    /**
     * Suspends at most 10s (waits for billing client to connect).
     * Since the [BillingClient] can only notify its state through the [connectionStateListener], we
     * poll the [connected] status. Ideally, we would collect a state flow..
     */
    private suspend fun connectWithRetry() = coroutineScope {
        connectClient()
        launch {
            var awaited = 0
            /* We wait at most 10 seconds */
            while (awaited < 10000) {
                if (connected) break else {
                    delay(10)
                    awaited += 10
                }
            }
        }
    }

    private suspend fun queryIgnLicenseDetails(): SkuQueryResult = suspendCoroutine<SkuQueryResult> {
        val skuList = ArrayList<String>()
        skuList.add(IGN_LICENSE_SUBSCRIPTION_SKU)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)
        billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            it.resume(SkuQueryResult(billingResult, skuDetailsList ?: listOf()))
        }
    }

    data class SkuQueryResult(val billingResult: BillingResult, val skuDetailsList: List<SkuDetails>)

    fun launchBilling(skuDetails: SkuDetails, purchaseAcknowledgedCb: PurchaseAcknowledgedCallback,
                      purchasePendingCb: PurchasePendingCallback): BillingParams {
        val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()
        this.purchaseAcknowledgedCallback = purchaseAcknowledgedCb
        this.purchasePendingCallback = purchasePendingCb
        return BillingParams(billingClient, flowParams)
    }
}

data class BillingParams(val billingClient: BillingClient, val flowParams: BillingFlowParams)

private const val TAG = "ign.Billing.kt"

