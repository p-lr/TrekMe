package com.peterlaurence.trekme.core.billing.domain.interactors

import com.peterlaurence.trekme.core.billing.di.IGN
import com.peterlaurence.trekme.core.billing.di.TrekmeExtended
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Checks whether the user has one of the two "extended offers": the simple one and the one with the
 * IGN option.
 */
class HasOneExtendedOfferInteractor @Inject constructor(
    @IGN
    private val extendedOfferWithIgnStateOwner: ExtendedOfferStateOwner,
    @TrekmeExtended
    private val extendedOfferStateOwner: ExtendedOfferStateOwner,
) {
    fun getPurchaseFlow(scope: CoroutineScope): StateFlow<Boolean> {
        val purchaseFlow: StateFlow<Boolean> = combine(
            extendedOfferWithIgnStateOwner.purchaseFlow,
            extendedOfferStateOwner.purchaseFlow
        ) { x, y ->
            x == PurchaseState.PURCHASED || y == PurchaseState.PURCHASED
        }.stateIn(scope, SharingStarted.Eagerly, initialValue = false)

        return purchaseFlow
    }
}