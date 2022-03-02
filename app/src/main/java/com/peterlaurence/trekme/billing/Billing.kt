package com.peterlaurence.trekme.billing

import android.app.Application
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

typealias PurchasePendingCallback = () -> Unit

/**
 * Manages a subscription along with a one-time purchase.
 * To access some functionality, a user should have an active subscription, or a valid one-time
 * purchase.
 *
 * @author P.Laurence on 06/08/2019
 */
class Billing(
        val application: Application,
        private val oneTimeSku: String,
        private val subSku: String,
        private val purchaseVerifier: PurchaseVerifier
) : PurchasesUpdatedListener {

    val purchaseAcknowledgedEvent = MutableSharedFlow<Unit>(0, 1, BufferOverflow.DROP_OLDEST)
    private val billingClient = BillingClient.newBuilder(application).setListener(this).enablePendingPurchases().build()

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
     * See also [awaitConnect], which suspends at most 10s.
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
                if (
                        it.skus.any { sku ->
                            sku == oneTimeSku || sku == subSku
                        }
                ) {
                    if (it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged) {
                        acknowledgePurchase(it)
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

    private fun acknowledgePurchase(purchase: Purchase) {
        /* Approve the payment */
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
        billingClient.acknowledgePurchase(acknowledgePurchaseParams) {
            if (it.responseCode == OK) {
                purchaseAcknowledgedEvent.tryEmit(Unit)
            }
        }
    }

    private fun shouldAcknowledgePurchase(purchase: Purchase): Boolean {
        return (purchase.skus.any { it == subSku || it == oneTimeSku })
                && purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged
    }

    /**
     * This is one of the first things to do. If either the one-time or the subscription are among
     * the purchases, check if it should be acknowledged. This call is required when the
     * acknowledgement wasn't done right after a billing flow (typically when the payment method is
     * slow and the user didn't wait the end of the procedure with the [onPurchasesUpdated] call).
     * So we can end up with a purchase which is in [Purchase.PurchaseState.PURCHASED] state but not
     * acknowledged.
     *
     * @return Whether acknowledgment as done or not.
     */
    suspend fun acknowledgePurchase(): Boolean {
        runCatching { awaitConnect() }.onFailure { return false }

        val inAppPurchases = queryPurchasesAsync(BillingClient.SkuType.INAPP)
        val oneTimeAck = inAppPurchases.second.getOneTimePurchase()?.let {
            if (shouldAcknowledgePurchase(it)) {
                acknowledge(it)
            } else false
        } ?: false

        val subs = queryPurchasesAsync(BillingClient.SkuType.SUBS)
        val subAck = subs.second.getSubPurchase()?.let {
            if (shouldAcknowledgePurchase(it)) {
                acknowledge(it)
            } else false
        } ?: false

        return oneTimeAck || subAck
    }

    suspend fun getPurchase(): Purchase? {
        runCatching { awaitConnect() }.onFailure { return null }

        val inAppPurchases = queryPurchasesAsync(BillingClient.SkuType.INAPP)
        val oneTimeLicense = inAppPurchases.second.getValidOneTimePurchase()?.let {
            if (purchaseVerifier.checkTime(it.purchaseTime) !is AccessGranted) {
                consume(it.purchaseToken)
                null
            } else it
        }

        if (oneTimeLicense == null) {
            val subs = queryPurchasesAsync(BillingClient.SkuType.SUBS)
            return subs.second.getValidSubPurchase()
        }
        return oneTimeLicense
    }

    private fun List<Purchase>.getOneTimePurchase(): Purchase? {
        return firstOrNull { it.skus.any { sku -> sku == oneTimeSku } }
    }

    private fun List<Purchase>.getSubPurchase(): Purchase? {
        return firstOrNull { it.skus.any { sku -> sku == subSku } }
    }

    private fun List<Purchase>.getValidOneTimePurchase(): Purchase? {
        return firstOrNull { it.skus.any { sku -> sku == oneTimeSku } && it.isAcknowledged }
    }

    private fun List<Purchase>.getValidSubPurchase(): Purchase? {
        return firstOrNull {
            it.skus.any { sku -> sku == subSku } &&
                    it.isAcknowledged
        }
    }

    private fun consume(token: String) {
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(token).build()
        billingClient.consumeAsync(consumeParams) { _, _ ->
            Log.i(TAG, "Consumed the purchase. It can now be bought again.")
        }
    }

    /**
     * Get the details of the subscription.
     * @throws [ProductNotFoundException], [NotSupportedException], [IllegalStateException]
     */
    suspend fun getSubDetails(): SubscriptionDetails {
        awaitConnect()
        val (billingResult, skuDetailsList) = querySubDetails()
        return when (billingResult.responseCode) {
            OK -> skuDetailsList.find { it.sku == subSku }?.let {
                SubscriptionDetails(it)
            } ?: throw ProductNotFoundException()
            FEATURE_NOT_SUPPORTED -> throw NotSupportedException()
            SERVICE_DISCONNECTED -> error("should retry")
            else -> error("other error")
        }
    }

    /**
     * Suspends at most 10s (waits for billing client to connect).
     * Since the [BillingClient] can only notify its state through the [connectionStateListener], we
     * poll the [connected] status. Ideally, we would collect the billing client state flow..
     */
    private suspend fun awaitConnect() {
        connectClient()

        var awaited = 0
        /* We wait at most 10 seconds */
        while (awaited < 10000) {
            if (connected) break else {
                delay(10)
                awaited += 10
            }
        }
    }

    private suspend fun querySubDetails(): SkuQueryResult = suspendCoroutine {
        val skuList = ArrayList<String>()
        skuList.add(subSku)
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.SUBS)
        billingClient.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            it.resume(SkuQueryResult(billingResult, skuDetailsList ?: listOf()))
        }
    }

    /**
     * Using a [callbackFlow] instead of [suspendCancellableCoroutine], as we have no way to remove
     * the provided callback given to [BillingClient.queryPurchasesAsync] - so creating a memory
     * leak.
     * By collecting a [callbackFlow], the real collector is on a different call stack. So the
     * [BillingClient] has no reference on the collector.
     */
    private suspend fun queryPurchasesAsync(skuType: String): Pair<BillingResult, List<Purchase>> = callbackFlow {
        billingClient.queryPurchasesAsync(skuType) { r, p ->
            trySend(Pair(r, p))
        }
        awaitClose { /* We can't do anything, but it doesn't matter */ }
    }.first()

    /**
     * Using a [callbackFlow] instead of [suspendCancellableCoroutine], as we have no way to remove
     * the provided callback given to [BillingClient.acknowledgePurchase] - so creating a memory
     * leak.
     * By collecting a [callbackFlow], the real collector is on a different call stack. So the
     * [BillingClient] has no reference on the collector.
     */
    private suspend fun acknowledge(purchase: Purchase) = callbackFlow {
        /* Approve the payment */
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(acknowledgePurchaseParams) {
            trySend(it.responseCode == OK)
        }

        awaitClose { /* We can't do anything, but it doesn't matter */ }
    }.first()

    data class SkuQueryResult(val billingResult: BillingResult, val skuDetailsList: List<SkuDetails>)

    fun launchBilling(skuDetails: SkuDetails, purchasePendingCb: PurchasePendingCallback): BillingParams {
        val flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build()
        this.purchasePendingCallback = purchasePendingCb
        return BillingParams(billingClient, flowParams)
    }
}

class NotSupportedException : Exception()
class ProductNotFoundException : Exception()

data class BillingParams(val billingClient: BillingClient, val flowParams: BillingFlowParams)

data class SubscriptionDetails(val skuDetails: SkuDetails) {
    val price: String
        get() = skuDetails.price
    val trialDuration: String
        get() = skuDetails.freeTrialPeriod.filter { it.isDigit() }
}

private const val TAG = "Billing.kt"

