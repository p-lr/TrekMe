package com.peterlaurence.trekme.features.map.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.core.billing.domain.interactors.HasOneExtendedOfferInteractor
import com.peterlaurence.trekme.core.excursion.domain.model.ExcursionWaypoint
import com.peterlaurence.trekme.core.excursion.domain.repository.ExcursionRepository
import com.peterlaurence.trekme.core.map.domain.models.ExcursionRef
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.Marker
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarkersManageViewModel @Inject constructor(
    private val mapRepository: MapRepository,
    private val excursionRepository: ExcursionRepository,
    hasOneExtendedOfferInteractor: HasOneExtendedOfferInteractor
) : ViewModel() {
    val hasExtendedOffer = hasOneExtendedOfferInteractor.getPurchaseFlow(viewModelScope)

    private val map: Map?
        get() = mapRepository.getCurrentMap()

    fun getMarkersFlow(): StateFlow<List<Marker>> {
        return map?.markers ?: MutableStateFlow(emptyList())
    }

    val excursionWaypointFlow by lazy {
        getExcursionWaypointsFlow()
    }

    private fun getExcursionWaypointsFlow(): StateFlow<kotlin.collections.Map<ExcursionRef, List<ExcursionWaypoint>>> {
        return channelFlow {
            launch {
                val waypointsForExcursion = mutableMapOf<ExcursionRef, List<ExcursionWaypoint>>()
                map?.excursionRefs?.collectLatest { refs ->
                    coroutineScope {
                        for (ref in refs) {
                            ref.visible.collectLatest l@{ visible ->
                                if (visible) {
                                    launch {
                                        excursionRepository.getWaypoints(ref.id)?.collect {
                                            waypointsForExcursion[ref] = it
                                            send(waypointsForExcursion)
                                        }
                                    }
                                } else {
                                    waypointsForExcursion.remove(ref)
                                    send(waypointsForExcursion)
                                }
                            }
                        }
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    }

    /**
     * TODO: test if this way of exposing state is better.
     */
    fun getExcursionWaypointsFlow2(): StateFlow<kotlin.collections.Map<ExcursionRef, StateFlow<List<ExcursionWaypoint>>>> {
        return channelFlow {
            launch {
                val waypointsForExcursion = mutableMapOf<ExcursionRef, StateFlow<List<ExcursionWaypoint>>>()
                map?.excursionRefs?.collectLatest { refs ->
                    coroutineScope {
                        for (ref in refs) {
                            ref.visible.collectLatest l@{ visible ->
                                if (visible) {
                                    launch {
                                        excursionRepository.getWaypoints(ref.id)?.also {
                                            waypointsForExcursion[ref] = it
                                            send(waypointsForExcursion)
                                        }
                                    }
                                } else {
                                    waypointsForExcursion.remove(ref)
                                    send(waypointsForExcursion)
                                }
                            }
                        }
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    }
}