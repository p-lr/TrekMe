package com.peterlaurence.trekme.features.map.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.billing.domain.interactors.HasOneExtendedOfferInteractor
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MarkersManageViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    hasOneExtendedOfferInteractor: HasOneExtendedOfferInteractor
) : ViewModel() {
    val hasExtendedOffer = hasOneExtendedOfferInteractor.getPurchaseFlow(viewModelScope)

    private val map: Map?
        get() = mapRepository.getCurrentMap()

    fun getMarkersFlow(): StateFlow<List<Marker>> {
        return map?.markers ?: MutableStateFlow(emptyList())
    }

}