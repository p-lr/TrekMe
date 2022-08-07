package com.peterlaurence.trekme.features.map.domain.interactors

import com.peterlaurence.trekme.core.billing.domain.model.PurchaseState
import com.peterlaurence.trekme.core.billing.domain.repositories.ExtendedOfferRepository
import com.peterlaurence.trekme.core.map.ErrorIgnLicense
import com.peterlaurence.trekme.core.map.FreeLicense
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.MapLicense
import com.peterlaurence.trekme.core.map.ValidIgnLicense
import com.peterlaurence.trekme.core.map.domain.models.Wmts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class MapLicenseInteractor @Inject constructor(
    private val extendedOfferRepository: ExtendedOfferRepository
) {
    fun getMapLicenseFlow(map: Map): Flow<MapLicense> = channelFlow {
        val origin = map.origin
        if (origin !is Wmts || !origin.licensed) {
            send(FreeLicense)
            return@channelFlow
        }

        extendedOfferRepository.updatePurchaseState()
        launch {
            extendedOfferRepository.purchaseFlow.collect {
                send(
                    if (PurchaseState.PURCHASED == extendedOfferRepository.purchaseFlow.value) {
                        ValidIgnLicense
                    } else {
                        ErrorIgnLicense(map)
                    }
                )
            }
        }
    }
}