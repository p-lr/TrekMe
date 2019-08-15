package com.peterlaurence.trekme.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.*
import com.peterlaurence.trekme.viewmodel.mapcreate.IgnLicenseDetails
import com.peterlaurence.trekme.viewmodel.mapcreate.NotSupportedException
import com.peterlaurence.trekme.viewmodel.mapcreate.ProductNotFoundException
import kotlinx.coroutines.delay
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val IGN_LICENSE_SKU = "ign_license"

class Billing(val context: Context, val activity: Activity) : PurchasesUpdatedListener, AcknowledgePurchaseResponseListener {

    private val billingClient = BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build()

    private lateinit var purchaseCallback: () -> Unit

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
                    println("Billing client is ready")
                } else {
                    it.resume(false)
                }
            }

            override fun onBillingServiceDisconnected() {
                println("Billing client disconnected")
            }
        })
    }

    override fun onPurchasesUpdated(billingResult: BillingResult?, purchases: MutableList<Purchase>?) {
        fun acknowledge() {
            purchases?.forEach {
                if (!it.isAcknowledged && it.sku == IGN_LICENSE_SKU) {
                    /* Approve the payment */
                    val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(it.purchaseToken)
                            .build()
                    billingClient.acknowledgePurchase(acknowledgePurchaseParams, this)

                    /* Then notify registered PurchaseListener that it completed normally */
                    if (this::purchaseCallback.isInitialized) {
                        purchaseCallback()
                    }
                }
            }
        }

        println("onPurchaseUpdated ${billingResult?.responseCode} count purchases ${purchases?.size}")
        if (billingResult != null && purchases != null) {
            if (billingResult.responseCode == OK) {
                acknowledge()
            }
        }
    }

    override fun onAcknowledgePurchaseResponse(billingResult: BillingResult?) {
        println("Acknowledgement of purchase : ${billingResult?.responseCode}")
    }

    suspend fun getIgnLicensePurchaseStatus(): Boolean {
        connectWithRetry()
        val purchases = billingClient.queryPurchases(BillingClient.SkuType.INAPP)

        /**
         * If if can't find the purchase we're looking for in the cached purchases, try to get it
         * from a network call.
         */
        if (purchases == null || !purchases.purchasesList.containsIgnLicense()) {
            return queryPurchaseStatusNetwork()
        }

        // consume
//        purchases.purchasesList.forEach {
//            if (it.sku == IGN_LICENSE_SKU) {
//                val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(it.purchaseToken).build()
//                billingClient.consumeAsync(consumeParams) { responseCode, outToken ->
//                    println("Consumed $responseCode")
//                }
//            }
//        }

        /**
         * Look into the cache since a network call isn't necessary.
         */
        return purchases.purchasesList.validateIgnLicense()

        // take into account pending transaction
        // probably return a enum with 3 possible states : PURCHASED, NOT_PURCHASED, PENDING
    }

    private suspend fun queryPurchaseStatusNetwork() = suspendCoroutine<Boolean> { cont ->
        billingClient.queryPurchaseHistoryAsync(BillingClient.SkuType.INAPP) { billingResult, purchaseHistoryRecordList ->
            if (billingResult.responseCode == OK) {
                purchaseHistoryRecordList.firstOrNull {
                    it.sku == IGN_LICENSE_SKU
                }?.let {
                    cont.resume(true)
                    return@queryPurchaseHistoryAsync
                }
            }
            cont.resume(false)
        }
    }

    private fun List<Purchase>.validateIgnLicense(): Boolean {
        return any { it.sku == IGN_LICENSE_SKU && it.isAcknowledged }
    }

    private fun List<Purchase>.containsIgnLicense(): Boolean {
        return any { it.sku == IGN_LICENSE_SKU }
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

    /**
     */
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

    fun launchBilling(skuDetails: SkuDetails, purchaseCallback: () -> Unit) {
        val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()
        this.purchaseCallback = purchaseCallback
        val responseCode = billingClient.launchBillingFlow(activity, flowParams)

        println("result of billing flow : ${responseCode.responseCode} ${responseCode.debugMessage}")
    }
}

