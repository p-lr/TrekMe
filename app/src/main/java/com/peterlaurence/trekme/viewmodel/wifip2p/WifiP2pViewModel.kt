package com.peterlaurence.trekme.viewmodel.wifip2p

import android.app.Application
import android.content.Intent
import androidx.lifecycle.*
import com.peterlaurence.trekme.core.wifip2p.Started
import com.peterlaurence.trekme.core.wifip2p.Stopped
import com.peterlaurence.trekme.core.wifip2p.WifiP2pService
import com.peterlaurence.trekme.core.wifip2p.WifiP2pState

/**
 *
 * @author P.Laurence on 07/04/20
 */
class WifiP2pViewModel(private val app: Application): AndroidViewModel(app) {

    val state: LiveData<WifiP2pState> = WifiP2pService.stateFlow.asLiveData()

    private val _errors = MutableLiveData<Errors>()
    val errors: LiveData<Errors> = _errors

    /**
     * Current user requests to receive a map (from another user)
     */
    fun onRequestReceive() {
        startService(WifiP2pService.StartAction.START_RCV)
    }

    /**
     * Current user requests to send a map (to another user)
     */
    fun onRequestSend() {
        startService(WifiP2pService.StartAction.START_SEND)
    }

    fun onRequestStop() {
        println("Stop request")
        val intent = Intent(app, WifiP2pService::class.java)
        intent.action = WifiP2pService.StopAction::class.java.name
        app.startService(intent)
    }

    private fun startService(action: WifiP2pService.StartAction) {
        val state = state.value

        /* If the service is stopped, this is the expected path */
        if (state == null || state == Stopped) {
            println("Receive request ${action.name}, state = $state")
            val intent = Intent(app, WifiP2pService::class.java)
            intent.action = action.name
            app.startService(intent)
            return
        }

        /* We're trying to start the service while it's already started */
        _errors.value = ServiceAlreadyStarted
    }
}

sealed class Errors
object ServiceAlreadyStarted: Errors()
