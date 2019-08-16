package com.peterlaurence.trekme.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.*
import com.peterlaurence.trekme.viewmodel.mapcreate.IgnLicenseDetails
import com.peterlaurence.trekme.viewmodel.mapcreate.NotSupportedException
import com.peterlaurence.trekme.viewmodel.mapcreate.ProductNotFoundException
import kotlinx.coroutines.delay
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val IGN_LICENSE_SKU = "ign_license"
//const val IGN_LICENSE_SKU = "android.test.purchased"

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
            return queryPurchaseStatusNetwork()?.let {
                if (!checkTime(it.purchaseTime)) {
                    it.consumeIgnLicense()
                    false
                } else true
            } ?: false
        }

        /**
         * Look into the cache since a network call isn't necessary.
         */
        return purchases.purchasesList.getValidIgnLicense()?.let {
            if (!checkTime(it.purchaseTime)) {
                it.consumeIgnLicense()
                false
            } else true
        } ?: false

        // take into account pending transaction
        // probably return a enum with 3 possible states : PURCHASED, NOT_PURCHASED, PENDING
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
        billingClient.consumeAsync(consumeParams) { responseCode, outToken ->
            println("Consumed $responseCode")
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

    /**
     * The billing API uses a purchase time in milliseconds since the epoch (Jan 1, 1970), which is
     * exactly the same as what we get with [Date.getTime].
     * So we obtain the current time in millis and convert the difference with the purchase time in
     * days. If the purchase is older than a year (365 days) or
     */
    private fun checkTime(timeMillis: Long): Boolean {
        val now = Date().time
        val millis = now - timeMillis
        return if (millis > 0) {
            TimeUnit.DAYS.convert(millis, TimeUnit.MILLISECONDS) <= 365
        } else {
            true    // purchase happened "in the future"
        }
    }
}

