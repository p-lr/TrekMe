package com.peterlaurence.trekme.billing

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

class Billing(val context: Context) : PurchasesUpdatedListener {

    private val billingClient = BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build()

//    private fun connectClient() {
//        billingClient = BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build()
//        billingClient.startConnection(object : BillingClientStateListener {
//            override fun onBillingSetupFinished(billingResult: BillingResult) {
//                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
//                    println("Billing client is ready")
//                }
//            }
//
//            override fun onBillingServiceDisconnected() {
//                println("Billing client disconnected")
//            }
//        })
//    }

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
        println("onPurchaseUpdated $billingResult")
        purchases?.forEach {
            println(it.sku)
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
            return queryPurchaseStatusNetwork()
        }

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
                IgnLicenseDetails(it.price)
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

}

