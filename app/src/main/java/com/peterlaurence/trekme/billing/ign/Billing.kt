package com.peterlaurence.trekme.billing.ign

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.*
import com.peterlaurence.trekme.viewmodel.mapcreate.IgnLicenseDetails
import com.peterlaurence.trekme.viewmodel.mapcreate.NotSupportedException
import com.peterlaurence.trekme.viewmodel.mapcreate.ProductNotFoundException
import kotlinx.coroutines.delay
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val IGN_LICENSE_SKU = "ign_license"
typealias PurchaseAcknowledged = () -> Unit

class Billing(val context: Context, val activity: Activity) : PurchasesUpdatedListener, AcknowledgePurchaseResponseListener {

    private val billingClient = BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build()

    private lateinit var purchaseAcknowledged: PurchaseAcknowledged

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
                if (billingResult.responseCode == OK) {
                    it.resume(true)
                } else {
                    it.resume(false)
                }
            }

            override fun onBillingServiceDisconnected() {
            }
        })
    }

    override fun onPurchasesUpdated(billingResult: BillingResult?, purchases: MutableList<Purchase>?) {
        fun acknowledge() {
            purchases?.forEach {
                if (!it.isAcknowledged && it.sku == IGN_LICENSE_SKU && it.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    /* Approve the payment */
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(it.purchaseToken)
                            .build()
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, this)
                }
            }
        }

        if (billingResult != null && purchases != null) {
            if (billingResult.responseCode == OK) {
                acknowledge()
            }
        }
    }

    /**
     * This is the callback of the [BillingClient.acknowledgePurchase] call.
     * The [purchaseAcknowledged] is called only if the purchase is successfully acknowledged.
     */
    override fun onAcknowledgePurchaseResponse(billingResult: BillingResult?) {
        /* Then notify registered PurchaseListener that it completed normally */
        billingResult?.also {
            if (it.responseCode == OK && this::purchaseAcknowledged.isInitialized) {
                purchaseAcknowledged()
            } else {
                Log.e(TAG, "Payment couldn't be acknowledged (code ${it.responseCode}): ${it.debugMessage}")
            }
        }
    }

    suspend fun getIgnLicensePurchaseStatus(): Boolean {
        connectWithRetry()
        val purchases = billingClient.queryPurchases(BillingClient.SkuType.INAPP)

        /**
         * If if can't find the purchase we're looking for in the cached purchases, try to get it
         * from a network call.
         */
        if (purchases == null || !purchases.purchasesList.containsIgnLicense()) {
            return queryPurchaseStatusNetwork()?.let {
                if (checkTime(it.purchaseTime) !is AccessGranted) {
                    it.consumeIgnLicense()
                    false
                } else true
            } ?: false
        }

        /**
         * Look into the cache since a network call isn't necessary.
         */
        return purchases.purchasesList.getValidIgnLicense()?.let {
            if (checkTime(it.purchaseTime) !is AccessGranted) {
                it.consumeIgnLicense()
                false
            } else true
        } ?: false
    }

    private suspend fun queryPurchaseStatusNetwork() = suspendCoroutine<PurchaseHistoryRecord?> { cont ->
        billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP) { billingResult, purchaseHistoryRecordList ->
            if (billingResult.responseCode == OK) {
                val purchase = purchaseHistoryRecordList.firstOrNull {
                    it.sku == IGN_LICENSE_SKU
                }
                cont.resume(purchase)
                return@queryPurchaseHistoryAsync
            }
            cont.resume(null)
        }
    }

    private fun List<Purchase>.getValidIgnLicense(): Purchase? {
        return firstOrNull { it.sku == IGN_LICENSE_SKU && it.isAcknowledged }
    }

    private fun List<Purchase>.containsIgnLicense(): Boolean {
        return any { it.sku == IGN_LICENSE_SKU }
    }

    private fun Purchase.consumeIgnLicense() {
        if (sku == IGN_LICENSE_SKU) {
            consume(purchaseToken)
        }
    }

    private fun PurchaseHistoryRecord.consumeIgnLicense() {
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
            it.resume(SkuQueryResult(billingResult, skuDetailsList))
        }
    }

    data class SkuQueryResult(val billingResult: BillingResult, val skuDetailsList: List<SkuDetails>)

    fun launchBilling(skuDetails: SkuDetails, purchaseAcknowledged: PurchaseAcknowledged) {
        val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()
        this.purchaseAcknowledged = purchaseAcknowledged
        billingClient.launchBillingFlow(activity, flowParams)
    }
}

const val TAG = "ign.Billing.kt"

