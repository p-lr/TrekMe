package com.peterlaurence.trekme.features.mapcreate.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.repositories.offers.extended.ExtendedOfferRepository
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.util.map
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ExtendedOfferViewModel @Inject constructor(
    repo: ExtendedOfferRepository,
) : ViewModel() {
    val purchaseStateFlow = repo.purchaseFlow
}
