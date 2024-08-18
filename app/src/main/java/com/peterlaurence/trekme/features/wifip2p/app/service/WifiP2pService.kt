package com.peterlaurence.trekme.features.wifip2p.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.map.data.models.MapFileBased
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.interactors.MapImportInteractor
import com.peterlaurence.trekme.core.map.domain.models.MapParseStatus
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.features.wifip2p.app.service.*
import com.peterlaurence.trekme.util.*
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@AndroidEntryPoint
class WifiP2pService : Service() {
    @Inject lateinit var mapRepository: MapRepository

    @Inject lateinit var mapImportInteractor: MapImportInteractor

    @Inject lateinit var appEventBus: AppEventBus

    @Inject
    @ApplicationContext
    lateinit var appContext: Context

    private var importedDir: File? = null
    private val notificationChannelId = "trekme.WifiP2pService"
    private val wifiP2pServiceNotificationId = 659531
    private val intentFilter = IntentFilter()
    private var mode: StartAction? = null
    private var channel: WifiP2pManager.Channel? = null
    private var manager: WifiP2pManager? = null
    private val peerListChannel = Channel<WifiP2pDeviceList>(capacity = 64)
    private var job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main)
    private var autoRestart: Job? = null

    private val serviceName = "_trekme_mapshare"
    private val serviceType = "_presence._tcp"
    private val listenPort = 8988

    private var isWifiP2pEnabled = false
    private var isDiscoveryActive = false
    private var serviceStarted = false

    private var wifiP2pState: WifiP2pState = _stateFlow.value
        get() = _stateFlow.value
        private set(value) {
            field = value
            _stateFlow.value = value
        }

    companion object {
        const val IMPORTED_PATH_ARG = "importedPath"
        const val MAP_ID_ARG = "mapId"
        private val _stateFlow = MutableStateFlow<WifiP2pState>(Stopped())
        val stateFlow = _stateFlow.asStateFlow()
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                        val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                        isWifiP2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                    }
                    WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION -> {
                        val state = intent.getIntExtra(WifiP2pManager.EXTRA_DISCOVERY_STATE, -1)
                        isDiscoveryActive = state == WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED
                    }
                    WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                        Log.d(TAG, "WifiP2p connection changed. Requesting connection info..")
                        val manager = manager ?: return
                        val channel = channel ?: return

                        manager.requestConnectionInfo(channel) { info ->
                            Log.d(TAG, "Got connection info $info")
                            /* We got called back, so cancel auto-restart */
                            autoRestart?.cancel()

                            if (info?.groupOwnerAddress == null) {
                                /* This matters while in sending mode */
                                if (mode is StartSend && wifiP2pState != Started) {
                                    Log.d(TAG, "Connection info is empty - go back to Started state")
                                    /* Go back to the started state */
                                    scope.launch {
                                        resetWifiP2p()
                                        initialize()
                                    }
                                }
                                return@requestConnectionInfo
                            }

                            /* At this point we consider ourselves connected */
                            wifiP2pState = P2pConnected

                            /* Unregister the broadcast receiver */
                            runCatching {
                                // May throw IllegalStateException
                                unregisterReceiver(receiver)
                            }

                            /* Immediately stop on-going P2P discovering operations */
                            val mode = mode ?: return@requestConnectionInfo
                            if (mode is StartSend) {
                                manager.clearServiceRequests(channel, null)
                                manager.stopPeerDiscovery(channel, null)
                            }
                            if (mode is StartRcv) {
                                manager.clearLocalServices(channel, null)
                                manager.stopPeerDiscovery(channel, null)
                            }

                            if (info.isGroupOwner) {
                                // server
                                if (mode is StartRcv) {
                                    serverReceives()
                                } else if (mode is StartSend) {
                                    serverSends(mode.map)
                                }
                            } else {
                                // client
                                val inetSocketAddress = InetSocketAddress(info.groupOwnerAddress?.hostAddress, listenPort)
                                if (mode is StartRcv) {
                                    clientReceives(inetSocketAddress)
                                } else if (mode is StartSend) {
                                    clientSends(inetSocketAddress, mode.map)
                                }
                            }
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

        appEventBus.requestNearbyWifiDevicesPerm()

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager

        /* register the BroadcastReceiver with the intent values to be matched  */
        registerReceiver(receiver, intentFilter)
    }

    /**
     * Accepts only three possible actions:
     * * [StartRcv]   -> Starts the service in receiving mode
     * * [StartSend]  -> Starts the service in sending mode
     * * [StopAction] -> Stops the service
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        if (action != null && action !in listOf(StartRcv::class.java.name, StartSend::class.java.name, StopAction::class.java.name)) {
            Log.e(TAG, "Illegal action sent to WifiP2pService")
            return START_NOT_STICKY
        }

        /* If the user used the notification action-stop button, stop the service */
        if (action == StopAction::class.java.name) {
            wifiP2pState = Stopping
            /* Unregister Android-specific listener */
            runCatching {
                // May throw IllegalStateException
                unregisterReceiver(receiver)
            }

            /* Stop the WifiP2p framework */
            scope.launch(NonCancellable) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                resetWifiP2p()

                /* Stop the service */
                serviceStarted = false
                scope.cancel()
                wifiP2pState = Stopped(ByUser)
                stopSelf()
            }

            return START_NOT_STICKY
        }

        val notificationBuilder = NotificationCompat.Builder(applicationContext, notificationChannelId)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.service_wifip2p_action))
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

        startForeground(wifiP2pServiceNotificationId, notificationBuilder.build())

        serviceStarted = true

        if (intent.action == StartRcv::class.java.name) {
            mode = StartRcv
            importedDir = intent.getStringExtra(IMPORTED_PATH_ARG)?.let { File(it) }
        }
        if (intent.action == StartSend::class.java.name) {
            val mapId = intent.getStringExtra(MAP_ID_ARG)?.let {
                runCatching { UUID.fromString(it) }.getOrNull()
            }
            val map = mapId?.let { mapRepository.getMap(mapId) }
            if (map != null) {
                mode = StartSend(map)
            } else {
                Log.e(TAG, "Couldn't find a map with the given id")
                stopSelf()
                return START_NOT_STICKY
            }
        }

        scope.launch {
            initialize()
        }

        return START_NOT_STICKY
    }

    /**
     * As per api 35 specification, stop the service on timeout.
     */
    override fun onTimeout(startId: Int) {
        super.onTimeout(startId)

        scope.launch {
            exitWithReason(Timeout, resetConnection = false)
        }
    }

    private suspend fun initialize() {
        channel = manager?.initialize(this.applicationContext, mainLooper) {
            Log.e(TAG, "Lost wifip2p connection")
        }

        /* Notify started */
        wifiP2pState = Started

        Log.d(TAG, "Starting peer discovery..")
        val channel = channel ?: return

        runCatching {
            manager?.discoverPeers(appContext, channel)
        }.onFailure {
            wifiP2pState = Stopped(WithError(WifiP2pServiceErrors.WIFIP2P_UNSUPPORTED))
            return
        }

        if (mode is StartRcv) {
            scope.launch {
                while (true) {
                    Log.d(TAG, "Re-advertise service")
                    launch {
                        startRegistration()
                    }
                    delay(15000)
                    manager?.clearLocalServices(channel)
                    manager?.stopPeerDiscovery(channel)
                    if (wifiP2pState != Started) break
                    manager?.discoverPeers(appContext, channel)
                }
            }
        }
        if (mode is StartSend) {
            scope.launch {
                while (true) {
                    launch {
                        val device = discoverReceivingDevice()
                        connectDevice(device)
                    }
                    delay(15000)
                    manager?.clearServiceRequests(channel).also { Log.d(TAG, "ClearServiceRequests $it") }
                    manager?.stopPeerDiscovery(channel)
                    if (wifiP2pState != Started) break
                    manager?.discoverPeers(appContext, channel)
                    Log.d(TAG, "Connection timeout - retry")
                }
            }
        }
    }

    private suspend fun startRegistration() {
        val serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(serviceName, serviceType, null)

        val channel = channel ?: return
        val manager = manager ?: return
        manager.addLocalService(appContext, channel, serviceInfo)
    }

    /**
     * Discovers the service which the receiving device exposes.
     * If the service is discovered, this suspending function resumes with a [WifiP2pDevice].
     * If anything goes wrong other than an IllegalStateException, the continuation is cancelled.
     * An IllegalStateException is thrown if WifiP2P isn't supported by the sending device.
     */
    private suspend fun discoverReceivingDevice(): WifiP2pDevice = suspendCancellableCoroutine { cont ->
        val channel = channel ?: return@suspendCancellableCoroutine
        val manager = manager ?: return@suspendCancellableCoroutine
        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { fullDomain, _, device ->
            Log.d(TAG, "Found a device advertising the right service")
            if (fullDomain.startsWith(serviceName)) {
                Log.d(TAG, device.deviceAddress)
                if (cont.isActive) cont.resume(device)
            }
        }

        val servListener = WifiP2pManager.DnsSdServiceResponseListener { _, _, _ ->
            // Don't care for now
        }

        manager.setDnsSdResponseListeners(channel, servListener, txtListener)

        /* Now that listeners are set, discover the service */
        val serviceRequest = WifiP2pDnsSdServiceRequest.newInstance(serviceName, serviceType)
        scope.launch {
            runCatching {
                Log.d(TAG, "Making service request..")
                val serviceAdded = manager.addServiceRequest(channel, serviceRequest)
                if (serviceAdded) {
                    Log.d(TAG, "Discovering services..")
                    manager.discoverServices(appContext, channel).also { success ->
                        if (success) {
                            Log.d(TAG, "Service successfully discovered")
                        } else {
                            // Something went wrong. Alert user - retry?
                            cont.cancel()
                        }
                    }
                } else {
                    // Something went wrong. Alert user - retry?
                    cont.cancel()
                }
            }.onFailure {
                cont.resumeWithException(IllegalStateException())
            }
        }
    }

    private suspend fun connectDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }
        val channel = channel ?: return
        manager?.connect(appContext, channel, config).also {
            /**
             * At this point, although the API has returned successfully, we still need to wait for
             * the broadcast event [WIFI_P2P_CONNECTION_CHANGED_ACTION] to be sure we're indeed
             * connected to the targeted device. We can infer this if the ip of the group owner is
             * not null.
             */
            wifiP2pState = AwaitingP2pConnection

            /* Avoid hanging in this state and auto restart after some delay */
            autoRestart = scope.launch {
                delay(20_000)
                if (wifiP2pState == AwaitingP2pConnection) {
                    Log.d(TAG, "Returning back to started state after timeout")
                    resetWifiP2p()
                    initialize()
                }
            }
        }
    }

    private fun serverReceives() = scope.launch(Dispatchers.IO) {
        wifiP2pState = AwaitingSocketConnection

        runCatching {
            val serverSocket = ServerSocket(listenPort)
            serverSocket.use {
                /* Wait for client connection. Blocks until a connection is accepted from a client */
                Log.d(TAG, "waiting for client connect..")
                val client = serverSocket.accept()

                wifiP2pState = SocketConnected

                /* The client is assumed to write into a DataOutputStream */
                val inputStream = DataInputStream(client.getInputStream())
                receive(inputStream)
            }
        }
        Log.d(TAG, "Server is done receiving")
    }

    private fun serverSends(map: Map) = scope.launch(Dispatchers.IO) {
        wifiP2pState = AwaitingSocketConnection

        runCatching {
            val serverSocket = ServerSocket(listenPort)
            serverSocket.soTimeout = 0
            serverSocket.use {
                /* Wait for client connection. Blocks until a connection is accepted from a client */
                Log.d(TAG, "Waiting for client connect..")
                val client = serverSocket.accept()

                wifiP2pState = SocketConnected
                val outputStream = DataOutputStream(client.getOutputStream())
                send(map, outputStream)
                /* Wait before closing the socket */
                delay(5000)
            }
        }
        Log.d(TAG, "Server is done sending")
    }

    private fun clientSends(socketAddress: InetSocketAddress, map: Map) = scope.launch(Dispatchers.IO) {
        Log.d(TAG, "Sending ${map.name} from client")
        wifiP2pState = AwaitingSocketConnection

        runCatching {
            val socket = Socket()
            socket.soTimeout = 0
            socket.bind(null)
            socket.connect(socketAddress)

            wifiP2pState = SocketConnected

            socket.use {
                val outputStream = DataOutputStream(socket.getOutputStream())
                send(map, outputStream)
                /* Wait before closing the socket */
                delay(5000)
            }
        }

        Log.d(TAG, "Client is done sending")
    }

    private fun clientReceives(socketAddress: InetSocketAddress) = scope.launch(Dispatchers.IO) {
        wifiP2pState = AwaitingSocketConnection

        runCatching {
            val socket = Socket()
            socket.soTimeout = 0
            socket.bind(null)
            socket.connect(socketAddress)
            wifiP2pState = SocketConnected

            socket.use {
                val inputStream = DataInputStream(socket.getInputStream())
                receive(inputStream)
            }
        }

        Log.d(TAG, "Client is done receiving")
    }

    private fun receive(inputStream: DataInputStream) {
        /* The name of the map is expected to be the first data */
        val mapName = inputStream.readUTF()
        Log.d(TAG, "Receiving $mapName..")

        /* The uncompressed size is expected to come second */
        val sizeUncompressed = inputStream.readLong()
        val dir = File(importedDir, mapName).unique()
        dir.mkdir()

        /* Finally comes the compressed stream */
        unzipTask(inputStream, dir, sizeUncompressed, false, object : UnzipProgressionListener {
            override fun onProgress(p: Int) {
                wifiP2pState = Loading(p)
            }

            override fun onUnzipFinished(outputDirectory: File, percent: Double) {
                /* Logically, the percent here should always be 100.0. Indeed, in case of failure,
                 * onUnzipError is invoked. However, shall we need that information in the future,
                 * we can get the precise percent of missing data. */
                wifiP2pState = Loading(percent.toInt())

                /* Import the map */
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        mapImportInteractor.importFromFile(dir)
                    }
                    /* The receiver resets the WifiP2P connection */
                    when (result.status) {
                        MapParseStatus.NEW_MAP,
                        MapParseStatus.EXISTING_MAP -> exitWithReason(
                            MapSuccessfullyLoaded(result.map?.name?.value ?: ""), true)
                        else -> exitWithReason(WithError(WifiP2pServiceErrors.MAP_IMPORT_ERROR), true)
                    }
                }
            }

            override fun onUnzipError() {
                scope.launch {
                    exitWithReason(WithError(WifiP2pServiceErrors.UNZIP_ERROR))
                }
            }
        })
    }

    private fun send(map: Map, outputStream: DataOutputStream) {
        /* The name of the map is expected to be the first data */
        outputStream.writeUTF(map.name.value)

        /* The uncompressed size is expected to come second */
        val directory = (map as? MapFileBased)?.folder ?: return
        val size = FileUtils.dirSize(directory)
        outputStream.writeLong(size)

        /* Finally comes the compressed stream */
        zipTask(directory, outputStream, object : ZipProgressionListener {
            override fun fileListAcquired() {
                wifiP2pState = Loading(0)
            }

            override fun onProgress(p: Int) {
                wifiP2pState = Loading(p)
            }

            override fun onZipFinished() {
                wifiP2pState = Loading(100)
                scope.launch {
                    /* The sender doesn't reset the WifiP2P connection on normal conditions (the
                     * receiver does) */
                    exitWithReason(MapSuccessfullyLoaded(map.name.value), false)
                }
            }

            override fun onZipError() {
                scope.launch {
                    /* The sender resets the connection on error */
                    exitWithReason(WithError(WifiP2pServiceErrors.UNZIP_ERROR), true)
                }
            }
        })
    }

    private suspend fun resetWifiP2p() {
        val manager = manager ?: return
        val channel = channel ?: return

        /* We don't care about the success or failure of this call, this service is going to
         * shutdown anyway. */
        manager.cancelConnect(channel).also { Log.d(TAG, "Cancel connect $it") }
        manager.clearLocalServices(channel).also { Log.d(TAG, "ClearLocalServices $it") }
        manager.clearServiceRequests(channel).also { Log.d(TAG, "ClearServiceRequests $it") }
        manager.removeGroup(channel).also { Log.d(TAG, "Remove group $it") }
        manager.stopPeerDiscovery(channel).also { Log.d(TAG, "Stop peer discovery $it") }
        peerListChannel.tryReceive()
    }

    private suspend fun exitWithReason(reason: StopReason, resetConnection: Boolean = false) {
        autoRestart?.cancel()
        wifiP2pState = Stopped(reason)
        if (resetConnection) resetWifiP2p()
        scope.cancel()
        stopSelf()
    }
}

/**
 * This service can be started in two ways.
 * Either in receiving or sending mode.
 */
sealed class StartAction
data object StartRcv : StartAction()
data class StartSend(val map: Map) : StartAction()

object StopAction

sealed class WifiP2pState : Comparable<WifiP2pState> {
    abstract val index: Int
    override fun compareTo(other: WifiP2pState): Int {
        if (this == other) return 0
        return if (index < other.index) -1 else 1
    }
}

data object Started : WifiP2pState() {
    override val index: Int = 0
}

data object AwaitingP2pConnection : WifiP2pState() {
    override val index: Int = 1
}

data object P2pConnected : WifiP2pState() {
    override val index: Int = 2
}

data object AwaitingSocketConnection : WifiP2pState() {
    override val index: Int = 3
}

data object SocketConnected : WifiP2pState() {
    override val index: Int = 4
}

data class Loading(val progress: Int) : WifiP2pState() {
    override val index: Int = 5
}

data object Stopping : WifiP2pState() {
    override val index: Int = 9
}

data class Stopped(val stopReason: StopReason? = null) : WifiP2pState() {
    override val index: Int = 10
}

sealed class StopReason
data object ByUser : StopReason()
data object Timeout : StopReason()
data class WithError(val error: WifiP2pServiceErrors) : StopReason()
data class MapSuccessfullyLoaded(val name: String) : StopReason()

enum class WifiP2pServiceErrors {
    UNZIP_ERROR, MAP_IMPORT_ERROR, WIFIP2P_UNSUPPORTED
}

/**
 * Appends a dash and a number so that the returned [File] is guaranteed to not exist (unique file).
 * Uses a recursive algorithm.
 */
private tailrec fun File.unique(): File {
    return if (!exists()) {
        this
    } else {
        val matchResult = regex.find(name)
        if (matchResult == null) {
            File("$path-1").unique()
        } else {
            val (basename, index) = matchResult.destructured
            val newIndex = index.toInt() + 1
            val newFile = File(parent, "$basename-$newIndex")
            newFile.unique()
        }
    }
}

private val regex = """(.*)-(\d+)""".toRegex()

private val TAG = WifiP2pService::class.java.name