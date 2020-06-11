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
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.mapimporter.MapImporter
import com.peterlaurence.trekme.core.map.maploader.MapLoader
import com.peterlaurence.trekme.util.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@AndroidEntryPoint
class WifiP2pService : Service() {
    @Inject lateinit var trekMeContext: TrekMeContext
    private val notificationChannelId = "trekme.WifiP2pService"
    private val wifiP2pServiceNotificationId = 659531
    private val intentFilter = IntentFilter()
    private var mode: StartAction? = null
    private var channel: WifiP2pManager.Channel? = null
    private var manager: WifiP2pManager? = null
    private val peerListChannel = Channel<WifiP2pDeviceList>(capacity = 64)
    private var job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main)

    private val serviceName = "_trekme_mapshare"
    private val serviceType = "_presence._tcp"
    private val listenPort = 8988

    private var isWifiP2pEnabled = false
    private var isDiscoveryActive = false
    private var isNetworkAvailable = false
    private var serviceStarted = false

    private var wifiP2pState: WifiP2pState = Stopped()
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
                            if (info?.groupOwnerAddress == null) {
                                /* This matters while in sending mode */
                                if (wifiP2pState != Started) {
                                    Log.d(TAG, "Connection info is empty - go back to Started state")
                                    /* Go back to the started state */
                                    wifiP2pState = Started
                                    scope.launch {
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

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerNetworkCallback(NetworkRequest.Builder().build(),
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        println("Network changed: available")
                        isNetworkAvailable = true
                    }

                    override fun onLost(network: Network) {
                        println("Network changed: lost")
                        isNetworkAvailable = false
                    }
                })

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
        if (intent.action !in listOf(StartRcv::class.java.name, StartSend::class.java.name, StopAction::class.java.name)) {
            Log.e(TAG, "Illegal action sent to WifiP2pService")
            return START_NOT_STICKY
        }

        /* If the user used the notification action-stop button, stop the service */
        if (intent.action == StopAction::class.java.name) {
            wifiP2pState = Stopping
            /* Unregister Android-specific listener */
            runCatching {
                // May throw IllegalStateException
                unregisterReceiver(receiver)
            }

            /* Stop the WifiP2p framework */
            scope.launch(NonCancellable) {
                stopForeground(true)
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

        startForeground(wifiP2pServiceNotificationId, notificationBuilder.build())

        serviceStarted = true

        if (intent.action == StartRcv::class.java.name) {
            mode = StartRcv
        }
        if (intent.action == StartSend::class.java.name) {
            val mapId = intent.getIntExtra("mapId", -1)
            val map = MapLoader.getMap(mapId)
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

    private suspend fun initialize() {
        channel = manager?.initialize(this, mainLooper) {
            Log.e(TAG, "Lost wifip2p connection")
        }

        /* Notify started */
        wifiP2pState = Started

        Log.d(TAG, "Starting peer discovery..")
        val channel = channel ?: return

        runCatching {
            manager?.discoverPeers(channel)
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
                    manager?.discoverPeers(channel)
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
                    manager?.discoverPeers(channel)
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
        manager.addLocalService(channel, serviceInfo)
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
        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { fullDomain, record, device ->
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
                    manager.discoverServices(channel).also { success ->
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
        manager?.connect(channel, config).also {
            /**
             * At this point, although the API has returned successfully, we still need to wait for
             * the broadcast event WIFI_P2P_CONNECTION_CHANGED_ACTION to be sure we're indeed
             * connected to the targeted device. We can infer this if the ip of the group owner is
             * not null.
             */
            wifiP2pState = AwaitingP2pConnection
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
            }
        }
        Log.d(TAG, "Server is done sending")
    }

    private fun clientSends(socketAddress: InetSocketAddress, map: Map) = scope.launch(Dispatchers.IO) {
        Log.d(TAG, "Sending ${map.name} from client")
        wifiP2pState = AwaitingSocketConnection

        runCatching {
            val socket = Socket()
            socket.bind(null)
            socket.connect(socketAddress)
            socket.soTimeout = 0

            wifiP2pState = SocketConnected

            socket.use {
                val outputStream = DataOutputStream(socket.getOutputStream())
                send(map, outputStream)
            }
        }

        Log.d(TAG, "Client is done sending")
    }

    private fun clientReceives(socketAddress: InetSocketAddress) = scope.launch(Dispatchers.IO) {
        wifiP2pState = AwaitingSocketConnection

        runCatching {
            val socket = Socket()
            socket.bind(null)
            socket.connect(socketAddress)
            socket.soTimeout = 0
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
        val dir = File(trekMeContext.importedDir, mapName).unique()
        dir.mkdir()

        /* Finally comes the compressed stream */
        unzipTask(inputStream, dir, sizeUncompressed, false, object : UnzipProgressionListener {
            override fun onProgress(p: Int) {
                println(p)
                wifiP2pState = Loading(p)
            }

            override fun onUnzipFinished(outputDirectory: File) {
                wifiP2pState = Loading(100)

                /* Import the map */
                scope.launch(Dispatchers.IO) {
                    val result = MapImporter.importFromFile(dir, Map.MapOrigin.VIPS)
                    result.map?.also { map ->
                        MapLoader.addMap(map)
                    }
                    when (result.status) {
                        MapImporter.MapParserStatus.NEW_MAP,
                        MapImporter.MapParserStatus.EXISTING_MAP -> exitWithReason(MapSuccessfullyLoaded(result.map?.name ?: ""))
                        else -> exitWithReason(WithError(WifiP2pServiceErrors.MAP_IMPORT_ERROR))
                    }
                }
            }

            override fun onUnzipError() {
                scope.launch {
                    exitWithReason(WithError(WifiP2pServiceErrors.UNZIP_ERROR))
                }
            }
        })
        runCatching {
            inputStream.close()
        }
    }

    private fun send(map: Map, outputStream: DataOutputStream) {
        /* The name of the map is expected to be the first data */
        outputStream.writeUTF(map.name)

        /* The uncompressed size is expected to come second */
        val size = FileUtils.dirSize(map.directory)
        outputStream.writeLong(size)

        /* Finally comes the compressed stream */
        zipTask(map.directory, outputStream, object : ZipProgressionListener {
            override fun fileListAcquired() {
                wifiP2pState = Loading(0)
            }

            override fun onProgress(p: Int) {
                wifiP2pState = Loading(p)
            }

            override fun onZipFinished() {
                wifiP2pState = Loading(100)
                scope.launch {
                    exitWithReason(MapSuccessfullyLoaded(map.name))
                }
            }

            override fun onZipError() {
                scope.launch {
                    exitWithReason(WithError(WifiP2pServiceErrors.UNZIP_ERROR))
                }
            }
        })
        runCatching {
            outputStream.close()
        }
    }

    private suspend fun resetWifiP2p() {
        val manager = manager ?: return
        val channel = channel ?: return

        /* We don't care about the success or failure of this call, this service is going to
         * shutdown anyway. */
        manager.cancelConnect(channel).also { println("Cancel connect $it") }
        manager.clearLocalServices(channel).also { println("ClearLocalServices $it") }
        manager.clearServiceRequests(channel).also { println("ClearServiceRequests $it") }
        manager.removeGroup(channel).also { println("Removegroup $it") }
        manager.stopPeerDiscovery(channel).also { println("Stop peer discovery $it") }
        peerListChannel.poll()
    }

    private suspend fun exitWithReason(reason: StopReason) {
        resetWifiP2p()
        wifiP2pState = Stopped(reason)
        stopSelf()
    }
}

/**
 * This service can be started in two ways.
 * Either in receiving or sending mode.
 */
sealed class StartAction
object StartRcv : StartAction()
data class StartSend(val map: Map) : StartAction()

object StopAction

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

object AwaitingP2pConnection : WifiP2pState() {
    override val index: Int = 1
}

object P2pConnected : WifiP2pState() {
    override val index: Int = 2
}

object AwaitingSocketConnection : WifiP2pState() {
    override val index: Int = 3
}

object SocketConnected : WifiP2pState() {
    override val index: Int = 4
}

data class Loading(val progress: Int) : WifiP2pState() {
    override val index: Int = 5
}

object Stopping : WifiP2pState() {
    override val index: Int = 9
}

data class Stopped(val stopReason: StopReason? = null) : WifiP2pState() {
    override val index: Int = 10
}

sealed class StopReason
object ByUser: StopReason()
data class WithError(val error: WifiP2pServiceErrors): StopReason()
data class MapSuccessfullyLoaded(val name: String): StopReason()

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
        val regex = """(.*)-(\d+)""".toRegex()
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

private val TAG = WifiP2pService::class.java.name