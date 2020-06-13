package com.peterlaurence.trekme.billing.ign

import android.app.Application
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.*
import com.peterlaurence.trekme.viewmodel.mapcreate.IgnLicenseDetails
import com.peterlaurence.trekme.viewmodel.mapcreate.NotSupportedException
import com.peterlaurence.trekme.viewmodel.mapcreate.ProductNotFoundException
import kotlinx.coroutines.delay
import org.greenrobot.eventbus.EventBus
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val IGN_LICENSE_SKU = "ign_license"
typealias PurchaseAcknowledgedCallback = () -> Unit
typealias PurchasePendingCallback = () -> Unit

class Billing(val application: Application) : PurchasesUpdatedListener, AcknowledgePurchaseResponseListener {

    private val billingClient = BillingClient.newBuilder(application).setListener(this).enablePendingPurchases().build()

    private lateinit var purchaseAcknowledgedCallback: PurchaseAcknowledgedCallback
    private lateinit var purchasePendingCallback: PurchasePendingCallback

    /**
     * This function returns when we're connected to the billing service.
     */
    private suspend fun connectClient() = suspendCoroutine<Boolean> {
        if (billingClient.isReady) {
            it.resume(true)
            return@suspendCoroutine
        }

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                /* It already happened that this continuation was called twice, resulting in IllegalStateException */
                try {
                    if (billingResult.responseCode == OK) {
                        it.resume(true)
                    } else {
                        it.resume(false)
                    }
                } catch (e: Exception) {
                }
            }

            override fun onBillingServiceDisconnected() {
            }
        })
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        fun acknowledge() {
            purchases?.forEach {
                if (it.sku == IGN_LICENSE_SKU) {
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
        return purchase.sku == IGN_LICENSE_SKU && purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged
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
        val purchases = billingClient.queryPurchases(BillingClient.SkuType.INAPP)

        return purchases.purchasesList?.getIgnLicense()?.let {
            this.purchaseAcknowledgedCallback = purchaseAcknowledgedCallback
            if (shouldAcknowledgeIgnLicense(it)) {
                acknowledgeIgnLicense(it)
                true
            } else false
        } ?: false
    }

    suspend fun getIgnLicensePurchase(): Purchase? {
        runCatching { connectWithRetry() }.onFailure { return null }
        val purchases = billingClient.queryPurchases(BillingClient.SkuType.INAPP)

        return purchases.purchasesList?.getValidIgnLicense()?.let {
            if (checkTime(it.purchaseTime) !is AccessGranted) {
                it.consumeIgnLicense()
                null
            } else it
        }
    }

    private fun List<Purchase>.getIgnLicense(): Purchase? {
        return firstOrNull { it.sku == IGN_LICENSE_SKU }
    }

    private fun List<Purchase>.getValidIgnLicense(): Purchase? {
        return firstOrNull { it.sku == IGN_LICENSE_SKU && it.isAcknowledged }
    }

    private fun Purchase.consumeIgnLicense() {
        if (sku == IGN_LICENSE_SKU) {
            consume(purchaseToken)
        }
    }

    private fun consume(token: String) {
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(token).build()
        billingClient.consumeAsync(consumeParams) { _, _ ->
            Log.i(TAG, "Consumed the purchase. It can now be bought again.")
        }
    }

    suspend fun getIgnLicenseDetails(): IgnLicenseDetails {
        connectWithRetry()
        val (billingResult, skuDetailsList) = queryIgnLicenseSku()
        return when (billingResult.responseCode) {
            OK -> skuDetailsList.find { it.sku == IGN_LICENSE_SKU }?.let {
                IgnLicenseDetails(it)
            } ?: throw ProductNotFoundException()
            FEATURE_NOT_SUPPORTED -> throw NotSupportedException()
            SERVICE_DISCONNECTED -> error("should retry")
            else -> error("other error")
        }
    }

    private suspend fun connectWithRetry() {
        var retryCnt = 0
        while (!connectClient()) {
            retryCnt++
            delay(1000)
            if (retryCnt > 5) error("Couldn't connect to billing client")
        }
    }

    private suspend fun queryIgnLicenseSku() = suspendCoroutine<SkuQueryResult> {
        val skuList = ArrayList<String>()
        skuList.add(IGN_LICENSE_SKU)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)
        billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            it.resume(SkuQueryResult(billingResult, skuDetailsList ?: listOf()))
        }
    }

    data class SkuQueryResult(val billingResult: BillingResult, val skuDetailsList: List<SkuDetails>)

    fun launchBilling(skuDetails: SkuDetails, purchaseAcknowledgedCb: PurchaseAcknowledgedCallback, purchasePendingCb: PurchasePendingCallback) {
        val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()
        this.purchaseAcknowledgedCallback = purchaseAcknowledgedCb
        this.purchasePendingCallback = purchasePendingCb
        EventBus.getDefault().post(BillingFlowEvent(billingClient, flowParams))
    }
}

data class BillingFlowEvent(val billingClient: BillingClient, val flowParams: BillingFlowParams)

const val TAG = "ign.Billing.kt"

