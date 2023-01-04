package com.peterlaurence.trekme.features.wifip2p.presentation.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.features.wifip2p.app.service.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.*
import javax.inject.Inject

/**
 * The view-model for the WifiP2P feature (map sharing).
 *
 * @author P.Laurence on 07/04/20
 */
@HiltViewModel
class WifiP2pViewModel @Inject constructor(
    private val trekMeContext: TrekMeContext,
    private val app: Application
) : ViewModel() {

    val state: StateFlow<WifiP2pState> = WifiP2pService.stateFlow

    private val _errors = Channel<Errors>(Channel.BUFFERED)
    val errors = _errors.receiveAsFlow()

    /**
     * Current user requests to receive a map (from another user)
     */
    fun onRequestReceive() {
        val state = state.value
        if (state is Stopped) {
            val importedPath = trekMeContext.importedDir?.absolutePath ?: return
            startService(
                StartRcv::class.java.name,
                mapOf(WifiP2pService.IMPORTED_PATH_ARG to importedPath)
            )
            return
        }
        /* We're trying to start the service while it's already started */
        _errors.trySend(ServiceAlreadyStarted)
    }

    /**
     * Current user requests to send a map (to another user)
     */
    fun onRequestSend(mapId: UUID) {
        val state = state.value
        if (state is Stopped) {
            startService(
                StartSend::class.java.name, mapOf(WifiP2pService.MAP_ID_ARG to mapId.toString())
            )
            return
        }
        /* We're trying to start the service while it's already started */
        _errors.trySend(ServiceAlreadyStarted)
    }

    fun onRequestStop() {
        startService(StopAction::class.java.name)
    }

    private fun startService(action: String, stringExtras: Map<String, String> = mapOf()) {
        val intent = Intent(app, WifiP2pService::class.java)
        intent.action = action
        for (pair in stringExtras) {
            intent.putExtra(pair.key, pair.value)
        }
        app.startService(intent)
    }
}

sealed class Errors
object ServiceAlreadyStarted : Errors()
