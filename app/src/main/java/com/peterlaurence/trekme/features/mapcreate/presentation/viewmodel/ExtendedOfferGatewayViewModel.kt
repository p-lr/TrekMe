package com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.billing.di.IGN
import com.peterlaurence.trekme.core.billing.di.TrekmeExtended
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExtendedOfferGatewayViewModel @Inject constructor(
    @IGN
    extendedOfferWithIgnStateOwner: ExtendedOfferStateOwner,
    @TrekmeExtended
    extendedOfferStateOwner: ExtendedOfferStateOwner,
) : ViewModel() {

    val extendedOfferWithIgnPurchaseStateFlow = extendedOfferWithIgnStateOwner.purchaseFlow
    val extendedOfferPurchaseStateFlow = extendedOfferStateOwner.purchaseFlow
}
