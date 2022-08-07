package com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.billing.domain.model.ExtendedOfferStateOwner
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExtendedOfferViewModel @Inject constructor(
    extendedOfferStateOwner: ExtendedOfferStateOwner,
) : ViewModel() {
    val purchaseStateFlow = extendedOfferStateOwner.purchaseFlow
}
