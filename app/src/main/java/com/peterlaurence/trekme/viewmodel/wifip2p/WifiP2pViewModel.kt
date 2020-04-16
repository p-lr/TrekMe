package com.peterlaurence.trekme.viewmodel.wifip2p

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import com.peterlaurence.trekme.core.wifip2p.*

/**
 *
 * @author P.Laurence on 07/04/20
 */
class WifiP2pViewModel(private val app: Application) : AndroidViewModel(app) {

    val state: LiveData<WifiP2pState> = WifiP2pService.stateFlow.asLiveData()

    private val _errors = MutableLiveData<Errors>()
    val errors: LiveData<Errors> = _errors

    /**
     * Current user requests to receive a map (from another user)
     */
    fun onRequestReceive() {
        val state = state.value
        if (state == null || state == Stopped) {
            val intent = Intent(app, WifiP2pService::class.java)
            intent.action = StartRcv::class.java.name
            app.startService(intent)
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
        if (state == null || state == Stopped) {
            val intent = Intent(app, WifiP2pService::class.java)
            intent.action = StartSend::class.java.name
            intent.putExtra("mapId", mapId)
            app.startService(intent)
            return
        }
        /* We're trying to start the service while it's already started */
        _errors.value = ServiceAlreadyStarted
    }

    fun onRequestStop() {
        val intent = Intent(app, WifiP2pService::class.java)
        intent.action = StopAction::class.java.name
        app.startService(intent)
    }
}

sealed class Errors
object ServiceAlreadyStarted : Errors()
