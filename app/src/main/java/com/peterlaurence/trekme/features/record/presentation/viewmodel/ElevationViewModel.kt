package com.peterlaurence.trekme.features.record.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.events.WarningMessage
import com.peterlaurence.trekme.features.record.domain.interactors.UpdateElevationGraphInteractor
import com.peterlaurence.trekme.features.record.domain.interactors.UpdateGeoRecordElevationsInteractor
import com.peterlaurence.trekme.features.record.domain.model.ElevationCorrectionErrorEvent
import com.peterlaurence.trekme.features.record.domain.model.ElevationData
import com.peterlaurence.trekme.features.record.domain.model.ElevationStateOwner
import com.peterlaurence.trekme.features.record.domain.model.NoNetworkEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * This view-model listens to the [ElevationStateOwner]'s state changes. Whenever the repository
 * notifies that a gpx file needs to be updated (through [ElevationData.needsUpdate]), this
 * view-model performs the update. When it happens, it means that the repository successfully
 * fetched elevation data from a trusted source.
 * By default, when a gpx file is created by TrekMe, the gpx file metadata indicates that the
 * elevation source is the GPS. When the repository requires an update, the elevation source is
 * necessarily different.
 *
 * This view-model also notifies the UI of other events coming from the [ElevationStateOwner], such
 * as when an error occurred, or when there's no network.
 *
 * @since 2020/12/13
 **/
@HiltViewModel
class ElevationViewModel @Inject constructor(
    private val elevationStateOwner: ElevationStateOwner,
    private val updateGeoRecordElevationsInteractor: UpdateGeoRecordElevationsInteractor,
    private val updateElevationGraphInteractor: UpdateElevationGraphInteractor,
    private val appEventBus: AppEventBus,
    private val app: Application,
) : ViewModel() {
    val elevationState = elevationStateOwner.elevationState

    init {
        viewModelScope.launch {
            elevationState.collect { state ->
                when (state) {
                    is ElevationData -> {
                        if (state.needsUpdate) {
                            updateGeoRecordElevationsInteractor.updateElevations(state)
                        }
                    }
                    else -> {
                    } // Nothing to do
                }
            }
        }

        viewModelScope.launch {
            elevationStateOwner.events.collect {
                val ctx = app.applicationContext
                when (it) {
                    ElevationCorrectionErrorEvent -> {
                        val msg = ctx.getString(R.string.elevation_correction_error)
                        appEventBus.postMessage(StandardMessage(msg, showLong = false))
                    }
                    is NoNetworkEvent -> {
                        val msg = if (!it.internetOk) {
                            ctx.getString(R.string.network_required)
                        } else {
                            ctx.getString(R.string.elevation_service_down)
                        }
                        appEventBus.postMessage(
                            WarningMessage(
                                ctx.getString(R.string.warning_title),
                                msg
                            )
                        )
                    }
                }
            }
        }
    }

    fun onUpdateGraph(id: UUID) = viewModelScope.launch {
        updateElevationGraphInteractor.updateElevationGraph(id)
    }
}
