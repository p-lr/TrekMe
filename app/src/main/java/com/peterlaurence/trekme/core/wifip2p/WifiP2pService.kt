package com.peterlaurence.trekme.core.wifip2p

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.peterlaurence.trekme.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.launch

class WifiP2pService : Service() {
    private val notificationChannelId = "peterlaurence.WifiP2pService"
    private val wifiP2pServiceNofificationId = 659531
    private val intentFilter = IntentFilter()
    private var channel: WifiP2pManager.Channel? = null
    private var manager: WifiP2pManager? = null
    private var job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main)

    private var isWifiP2pEnabled = false
        private set(value) {
            field = value
//            if (!value) resetWifiP2p()
        }

    private var isDiscoveryActive = false

    private var isNetworkAvailable = false

    enum class StartAction {
        START_SEND, START_RCV
    }

    object StopAction

    private var serviceStarted = false

    private var wifiP2pState: WifiP2pState = Stopped
        private set(value) {
            field = value
            stateChannel.offer(value)
        }

    companion object {
        private val stateChannel = ConflatedBroadcastChannel<WifiP2pState>()
        val stateFlow: Flow<WifiP2pState> = stateChannel.asFlow()
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                        val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                        isWifiP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                        println("Wifi P2p enabled: $isWifiP2pEnabled")
                    }
                    WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                        val state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1)
                        isDiscoveryActive = state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED
                    }
                    WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                        println("Peers changed")
                        /* Request available peers */
                        val channel = channel ?: return
                        scope.launch {
                            val peers = manager?.requestPeers(channel)
                            // we have a list of peers - should display in a list ?
                        }
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        // We don't provide binding, so return null
        return null
    }

    override fun onCreate() {
        super.onCreate()

        println("CREATE")
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerNetworkCallback(NetworkRequest.Builder().build(),
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        isNetworkAvailable = true
                    }

                    override fun onLost(network: Network) {
                        isNetworkAvailable = false
                    }
                })

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager

        /* register the BroadcastReceiver with the intent values to be matched  */
        registerReceiver(receiver, intentFilter)
    }

    /**
     * Accepts only three possible actions:
     * * [StartAction.START_RCV]  -> Starts the service in receiving mode
     * * [StartAction.START_SEND] -> Starts the service in sending mode
     * * [StopAction]             -> Stops the service
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action !in (StartAction.values().map { it.name } + StopAction::class.java.name)) {
            Log.e(TAG, "Illegal action sent to WifiP2pService")
            return START_NOT_STICKY
        }

        /* If the user used the notification action-stop button, stop the service */
        if (intent.action == StopAction::class.java.name) {
            /* Unregister Android-specific listener */
            runCatching {
                // May throw IllegalStateException
                unregisterReceiver(receiver)
            }

            /* Stop the WifiP2p framework */
            scope.launch {
                stopForeground(true)
                resetWifiP2p()

                /* Stop the service */
                serviceStarted = false
                stopSelf()
            }

            return START_NOT_STICKY
        }

        val notificationBuilder = NotificationCompat.Builder(applicationContext, notificationChannelId)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.service_location_action))
                .setSmallIcon(R.drawable.ic_share_black_24dp)
                .setOngoing(true)

        /* This is only needed on Devices on Android O and above */
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val chan = NotificationChannel(notificationChannelId, getText(R.string.service_wifip2p_name), NotificationManager.IMPORTANCE_DEFAULT)
            chan.enableLights(true)
            chan.lightColor = Color.YELLOW
            notificationManager.createNotificationChannel(chan)
            notificationBuilder.setChannelId(notificationChannelId)
        }

        startForeground(wifiP2pServiceNofificationId, notificationBuilder.build())

        serviceStarted = true

        scope.launch {
            runCatching {
                initialize()
                if (intent.action == StartAction.START_RCV.name) {
                    startRegistration()
                }
                if (intent.action == StartAction.START_SEND.name) {
                    discoverService()
                }
            }.onFailure {
                // Warn the user that Wifi P2P isn't supported
            }
        }

        return START_NOT_STICKY
    }

    private suspend fun resetWifiP2p() {
        val manager = manager ?: return
        val channel = channel ?: return

        /* We don't care about the success or failure of this call, this service is going to
         * shutdown anyway. */
        manager.cancelConnect(channel).also { println("Cancel connect $it") }
        manager.clearLocalServices(channel).also { println("ClearLocalServices $it") }
        manager.clearServiceRequests(channel).also { println("ClearServiceRequests $it") }

        wifiP2pState = Stopped
    }

    private suspend fun initialize() {
        channel = manager?.initialize(this, mainLooper) {
            channel = null

            /* Notify stopped */
            wifiP2pState = Stopped
        }

        /* Notify started */
        wifiP2pState = Started

        println("Starting peer discovery..")
        val channel = channel ?: return
        manager?.discoverPeers(channel)
    }

    private suspend fun startRegistration() {
        val record: Map<String, String> = mapOf(
                "listenport" to 8988.toString(),
                "available" to "visible"
        )

        val serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance("_trekme_mapshare", "_presence._tcp", record)

        val channel = channel ?: return
        val manager = manager ?: return
        manager.addLocalService(channel, serviceInfo).also { success ->
            if (success) {
                println("Local service SUCCESS")
            } else {
                println("Local service FAIL")
            }
        }
    }


    private suspend fun discoverService() {
        val channel = channel ?: return
        val manager = manager ?: return
        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { fullDomain, record, device ->
            println("First listener")
            Log.d(TAG, "DnsSdTxtRecord available -$record")
            if (fullDomain.startsWith("_trekme_mapshare")) {
                record["listenport"]?.also {
                    println(device.deviceAddress)
                    // then connect to the device
                }
            }
        }

        val servListener = WifiP2pManager.DnsSdServiceResponseListener { _, _, _ ->
            // Don't care for now
        }

        manager.setDnsSdResponseListeners(channel, servListener, txtListener)

        /* Now that listeners are set, discover the service */
        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance("_trekme_mapshare", "_presence._tcp")
        val serviceAdded = manager.addServiceRequest(channel, serviceRequest)
        if (serviceAdded) {
            manager.discoverServices(channel).also { success ->
                if (success) {
                    println("Service successfully discovered")
                }
            }
        }
    }
}

sealed class WifiP2pState : Comparable<WifiP2pState> {
    abstract val index: Int
    override fun compareTo(other: WifiP2pState): Int {
        if (this == other) return 0
        return if (index < other.index) -1 else 1
    }
}

object Started : WifiP2pState() {
    override val index: Int = 0
}

object Stopped : WifiP2pState() {
    override val index: Int = 10
}

private val TAG = WifiP2pService::class.java.name