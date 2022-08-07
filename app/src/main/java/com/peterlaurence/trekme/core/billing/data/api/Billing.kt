package com.peterlaurence.trekme.core.billing.data.api

import android.app.Application
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode.*
import com.peterlaurence.trekme.core.billing.data.model.BillingParams
import com.peterlaurence.trekme.core.billing.domain.api.BillingApi
import com.peterlaurence.trekme.core.billing.domain.model.*
import com.peterlaurence.trekme.events.AppEventBus
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * Manages a subscription along with a one-time purchase.
 * To access some functionality, a user should have an active subscription, or a valid one-time
 * purchase.
 *
 * @since 2019/08/06
 */
class Billing(
    val application: Application,
    private val oneTimeId: String,
    private val subIdList: List<String>,
    private val purchaseVerifier: PurchaseVerifier,
    private val appEventBus: AppEventBus
) : BillingApi {

    override val purchaseAcknowledgedEvent =
        MutableSharedFlow<Unit>(0, 1, BufferOverflow.DROP_OLDEST)

    private lateinit var purchasePendingCallback: () -> Unit

    private var connected = false
    private val connectionStateListener = object : BillingClientStateListener {
        override fun onBillingSetupFinished(billingResult: BillingResult) {
            connected = billingResult.responseCode == OK
        }

        override fun onBillingServiceDisconnected() {
            connected = false
        }
    }

    private val purchaseUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        fun acknowledge() {
            purchases?.forEach {
                if (
                    it.products.any { id ->
                        id == oneTimeId || id in subIdList
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

    private val productDetailsForId = mutableMapOf<UUID, ProductDetails>()

    private val billingClient =
        BillingClient.newBuilder(application).setListener(purchaseUpdatedListener)
            .enablePendingPurchases().build()

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
        return (purchase.products.any { it == oneTimeId })
                && purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged
    }

    private fun shouldAcknowledgeSubPurchase(purchase: Purchase): Boolean {
        return (purchase.products.any { it in subIdList })
                && purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged
    }

    /**
     * This is one of the first things to do. If either the one-time or the subscription are among
     * the purchases, check if it should be acknowledged. This call is required when the
     * acknowledgement wasn't done right after a billing flow (typically when the payment method is
     * slow and the user didn't wait the end of the procedure with the [purchaseUpdatedListener] call).
     * So we can end up with a purchase which is in [Purchase.PurchaseState.PURCHASED] state but not
     * acknowledged.
     *
     * @return Whether acknowledgment as done or not.
     */
    override suspend fun acknowledgePurchase(): Boolean {
        runCatching { awaitConnect() }.onFailure { return false }

        val inAppQuery = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val inAppPurchases = queryPurchasesAsync(inAppQuery)
        val oneTimeAck = inAppPurchases.second.getOneTimePurchase()?.let {
            if (shouldAcknowledgePurchase(it)) {
                acknowledge(it)
            } else false
        } ?: false

        val subQuery = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val subs = queryPurchasesAsync(subQuery)
        val subAck = subs.second.getSubPurchase()?.let {
            if (shouldAcknowledgeSubPurchase(it)) {
                acknowledge(it)
            } else false
        } ?: false

        return oneTimeAck || subAck
    }

    override suspend fun isPurchased(): Boolean {
        runCatching { awaitConnect() }.onFailure { return false }

        val inAppQuery = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val inAppPurchases = queryPurchasesAsync(inAppQuery)
        val oneTimeLicense = inAppPurchases.second.getValidOneTimePurchase()?.let {
            if (purchaseVerifier.checkTime(it.purchaseTime) !is AccessGranted) {
                consume(it.purchaseToken)
                null
            } else it
        }

        return if (oneTimeLicense == null) {
            val subQuery = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            val subs = queryPurchasesAsync(subQuery)
            subs.second.getValidSubPurchase() != null
        } else true
    }

    private fun List<Purchase>.getOneTimePurchase(): Purchase? {
        return firstOrNull { it.products.any { id -> id == oneTimeId } }
    }

    private fun List<Purchase>.getSubPurchase(): Purchase? {
        return firstOrNull { it.products.any { id -> id in subIdList } }
    }

    private fun List<Purchase>.getValidOneTimePurchase(): Purchase? {
        return firstOrNull { it.products.any { id -> id == oneTimeId } && it.isAcknowledged }
    }

    private fun List<Purchase>.getValidSubPurchase(): Purchase? {
        return firstOrNull {
            it.products.any { id -> id in subIdList } &&
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
     * Get the details of a subscription.
     * @throws [ProductNotFoundException], [NotSupportedException], [IllegalStateException]
     */
    override suspend fun getSubDetails(index: Int): SubscriptionDetails {
        val subId = subIdList.getOrNull(index) ?: error("no sku for index $index")
        awaitConnect()
        val (billingResult, skuDetailsList) = querySubDetails(subId)
        return when (billingResult.responseCode) {
            OK -> skuDetailsList.find { it.productId == subId }?.let {
                makeSubscriptionDetails(it)
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

    private suspend fun querySubDetails(subId: String): ProductDetailsResult = suspendCoroutine {
        val productList =
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(subId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )

        val params = QueryProductDetailsParams.newBuilder().setProductList(productList)
        billingClient.queryProductDetailsAsync(params.build()) { billingResult,
                                                                 productDetailsList ->
            it.resume(ProductDetailsResult(billingResult, productDetailsList))
        }
    }

    /**
     * Using a [callbackFlow] instead of [suspendCancellableCoroutine], as we have no way to remove
     * the provided callback given to [BillingClient.queryPurchasesAsync] - so creating a memory
     * leak.
     * By collecting a [callbackFlow], the real collector is on a different call stack. So the
     * [BillingClient] has no reference on the collector.
     */
    private suspend fun queryPurchasesAsync(params: QueryPurchasesParams): Pair<BillingResult, List<Purchase>> =
        callbackFlow {
            billingClient.queryPurchasesAsync(params) { r, p ->
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

    private data class ProductDetailsResult(
        val billingResult: BillingResult,
        val productDetailsList: List<ProductDetails>
    )

    override fun launchBilling(
        id: UUID,
        purchasePendingCb: () -> Unit
    ) {
        val productDetails = productDetailsForId[id] ?: return
        val offerToken = productDetails.subscriptionOfferDetails?.get(0)?.offerToken ?: return
        val productDetailsParamsList =
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        val flowParams =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

        val billingParams = BillingParams(billingClient, flowParams)

        /* Since we need an Activity to start the billing flow, we send an event which the activity
         * is listening */
        appEventBus.startBillingFlow(billingParams)
    }

    private fun makeSubscriptionDetails(productDetails: ProductDetails): SubscriptionDetails? {
        /**
         * Trial periods are given in the form "P1W" -> 1 week, or "P4D" -> 4 days.
         */
        fun parseTrialPeriodInDays(period: String): Int {
            if (period.isEmpty()) return 0
            val qty = period.filter { it.isDigit() }.toInt()
            return when (period.lowercase().last()) {
                'w' -> qty * 7
                'd' -> qty
                else -> qty
            }
        }

        /* Assign an id and remember it (needed for purchase) */
        val id = UUID.randomUUID()
        productDetailsForId[id] = productDetails

        /* For the moment, we only support the base plan */
        val offer = productDetails.subscriptionOfferDetails?.firstOrNull() ?: return null

        /* The trial is the first pricing phase with 0 as price amount */
        val trialData =
            offer.pricingPhases.pricingPhaseList.firstOrNull { it.priceAmountMicros == 0L }
        val trialInfo = if (trialData != null) {
            TrialAvailable(trialDurationInDays = parseTrialPeriodInDays(trialData.billingPeriod))
        } else {
            TrialUnavailable
        }

        /* The "real" price phase is the first phase with a price other than 0 */
        val realPricePhase = offer.pricingPhases.pricingPhaseList.firstOrNull {
            it.priceAmountMicros != 0L
        } ?: return null

        return SubscriptionDetails(
            id = id,
            price = realPricePhase.formattedPrice,
            trialInfo = trialInfo
        )
    }
}

private const val TAG = "Billing.kt"

