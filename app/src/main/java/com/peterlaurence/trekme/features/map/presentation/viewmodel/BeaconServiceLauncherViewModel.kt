package com.peterlaurence.trekme.features.map.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.WarningMessage
import com.peterlaurence.trekme.features.map.presentation.events.MapFeatureEvents
import com.peterlaurence.trekme.util.android.isBackgroundLocationGranted
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BeaconServiceLauncherViewModel @Inject constructor(
    mapFeatureEvents: MapFeatureEvents,
    appEventBus: AppEventBus,
    @ApplicationContext appContext: Context
): ViewModel() {

    private val _startServiceEvent = Channel<Unit>(1)
    val startServiceEvent = _startServiceEvent.receiveAsFlow()

    init {
        viewModelScope.launch {
            mapFeatureEvents.hasBeaconsFlow.collectLatest {
                if (!isBackgroundLocationGranted(appContext)) {
                    val request = AppEventBus.BackgroundLocationRequest(
                        R.string.beacon_background_loc_perm,
                    )

                    appEventBus.requestBackgroundLocation(
                        request
                    )

                    request.result.receiveAsFlow().collect { granted ->
                        if (granted) {
                            _startServiceEvent.send(Unit)
                        } else {
                            appEventBus.postMessage(
                                WarningMessage(
                                    title = appContext.getString(R.string.warning_title),
                                    msg = appContext.getString(R.string.beacon_background_loc_perm_failure)
                                )
                            )
                        }
                    }
                } else {
                    _startServiceEvent.send(Unit)
                }
            }
        }
    }
}