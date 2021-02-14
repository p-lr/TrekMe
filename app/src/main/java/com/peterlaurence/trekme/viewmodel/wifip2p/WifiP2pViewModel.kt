package com.peterlaurence.trekme.viewmodel.wifip2p

import android.app.Application
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.wifip2p.*
import dagger.hilt.android.lifecycle.HiltViewModel
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

    val state: LiveData<WifiP2pState> = WifiP2pService.stateFlow.asLiveData()

    private val _errors = MutableLiveData<Errors>()
    val errors: LiveData<Errors> = _errors

    /**
     * Current user requests to receive a map (from another user)
     */
    fun onRequestReceive() {
        val state = state.value
        if (state == null || state is Stopped) {
            val importedPath = trekMeContext.importedDir?.absolutePath ?: return
            startService(StartRcv::class.java.name,
                    mapOf(WifiP2pService.IMPORTED_PATH_ARG to importedPath))
            return
        }
        /* We're trying to start the service while it's already started */
        _errors.value = ServiceAlreadyStarted
    }

    /**
     * Current user requests to send a map (to another user)
     */
    fun onRequestSend(mapId: Int) {
        val state = state.value
        if (state == null || state is Stopped) {
            startService(StartSend::class.java.name, mapOf(),
                    mapOf(WifiP2pService.MAP_ID_ARG to mapId))
            return
        }
        /* We're trying to start the service while it's already started */
        _errors.value = ServiceAlreadyStarted
    }

    fun onRequestStop() {
        startService(StopAction::class.java.name)
    }

    private fun startService(action: String, stringExtras: Map<String, String> = mapOf(),
                             intExtras: Map<String, Int> = mapOf()) {
        val intent = Intent(app, WifiP2pService::class.java)
        intent.action = action
        for (pair in stringExtras) {
            intent.putExtra(pair.key, pair.value)
        }
        for (pair in intExtras) {
            intent.putExtra(pair.key, pair.value)
        }
        app.startService(intent)
    }
}

sealed class Errors
object ServiceAlreadyStarted : Errors()
