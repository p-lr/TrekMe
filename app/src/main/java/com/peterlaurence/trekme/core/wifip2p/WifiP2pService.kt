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
import com.peterlaurence.trekme.util.UnzipProgressionListener
import com.peterlaurence.trekme.util.unzipTask
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WifiP2pService : Service() {
    private val notificationChannelId = "peterlaurence.WifiP2pService"
    private val wifiP2pServiceNofificationId = 659531
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

        private val errorChannel = ConflatedBroadcastChannel<WifiP2pServiceErrors>()
        val errorFlow: Flow<WifiP2pServiceErrors> = errorChannel.asFlow()
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
//                        val channel = channel ?: return
                        /* Request available peers only if we are starting */
//                        if (wifiP2pState == Started) {
//                            scope.launch {
//                                val peers = manager?.requestPeers(channel)
//                                if (peers != null) peerListChannel.offer(peers)
//                                // we have a list of peers - should display in a list ?
//                            }
//                        }
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

                            /* Immediately stop on-going P2P discovering operations */
                            if (mode == StartAction.START_SEND) {
                                manager.clearServiceRequests(channel, null)
                                manager.stopPeerDiscovery(channel, null)
                            }
                            if (mode == StartAction.START_RCV) {
                                manager.clearLocalServices(channel, null)
                                manager.stopPeerDiscovery(channel, null)
                            }

                            if (info.isGroupOwner) {
                                // server
                                if (mode!! == StartAction.START_RCV) {
                                    serverReceives()
                                } else {
                                    serverSends()
                                }
                            } else {
                                // client
                                val inetSocketAddress = InetSocketAddress(info.groupOwnerAddress?.hostAddress, listenPort)
                                if (mode!! == StartAction.START_RCV) {
                                    clientReceives(inetSocketAddress)
                                } else {
                                    clientSends(inetSocketAddress)
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
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
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
                wifiP2pState = Stopped
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

        if (intent.action == StartAction.START_RCV.name) {
            mode = StartAction.START_RCV
        }
        if (intent.action == StartAction.START_SEND.name) {
            mode = StartAction.START_SEND
        }

        scope.launch {
            initialize()
        }

        return START_NOT_STICKY
    }

    private suspend fun initialize() {
        channel = manager?.initialize(this, mainLooper) {
            // TODO: react on this
        }

        /* Notify started */
        wifiP2pState = Started

        Log.d(TAG, "Starting peer discovery..")
        val channel = channel ?: return
        manager?.discoverPeers(channel)

        if (mode == StartAction.START_RCV) {
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
        if (mode == StartAction.START_SEND) {
            scope.launch {
                while (true) {
                    launch {
                        val device = discoverReceivingDevice()
                        connectDevice(device)
                    }
                    delay(15000)
                    manager?.clearServiceRequests(channel).also { println("ClearServiceRequests $it") }
                    manager?.stopPeerDiscovery(channel)
                    if (wifiP2pState != Started) break
                    manager?.discoverPeers(channel)
                    Log.d(TAG, "Connection timeout - retry")
                }
            }
        }
    }

    private suspend fun startRegistration() {
        val record: Map<String, String> = mapOf(
                "listenport" to listenPort.toString(),
                "available" to "visible"
        )

        val serviceInfo =
                WifiP2pDnsSdServiceInfo.newInstance(serviceName, "_presence._tcp", record)

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
            Log.d(TAG, "DnsSdTxtRecord available -$record")
            if (fullDomain.startsWith(serviceName)) {
                record["listenport"]?.also {
                    println(device.deviceAddress)
                    if (cont.isActive) cont.resume(device)
                }
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
        val serverSocket = ServerSocket(listenPort)
        serverSocket.use {
            /* Wait for client connection. Blocks until a connection is accepted from a client */
            Log.d(TAG, "waiting for client connect..")
            val client = serverSocket.accept()
            wifiP2pState = SocketConnected

            /* The client is assumed to write into a DataOutputStream */
            val inputStream = DataInputStream(client.getInputStream())

            val mapName = inputStream.readUTF()
            println("Recieving $mapName from server")
            val size = inputStream.readLong()
            println("Size: $size")

            var c = 0L
            var x = 0
            val myOutput = object : OutputStream() {
                override fun write(b: Int) {
                    x++
                    val percent = c++.toFloat() / size
                    if (x > DEFAULT_BUFFER_SIZE) {
                        println(percent)
                        x = 0
                    }
                }
            }

            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var bytes = inputStream.read(buffer)
            while (bytes >= 0 && isActive) {
                myOutput.write(buffer, 0, bytes)
                try {
                    bytes = inputStream.read(buffer)
                } catch (e: SocketException) {
                    break
                }
            }
            inputStream.close()
            myOutput.close()
            serverSocket.close()
        }
        println("Closed server socket")
    }

    private fun serverSends() = scope.launch(Dispatchers.IO) {
        wifiP2pState = AwaitingSocketConnection
        val serverSocket = ServerSocket(listenPort)
        serverSocket.use {
            /* Wait for client connection. Blocks until a connection is accepted from a client */
            println("waiting for client connect..")
            val client = serverSocket.accept()

            wifiP2pState = SocketConnected
            val outputStream = DataOutputStream(client.getOutputStream())
            val archivesDir = File(TrekMeContext.defaultAppDir, "archives")
            archivesDir.listFiles()?.firstOrNull()?.also {
                println("Sending ${it.name}")
                outputStream.writeUTF(it.name)
                val totalByteCount = it.length()
                outputStream.writeLong(totalByteCount)
                FileInputStream(it).use {
                    it.copyToWithProgress(outputStream, this, totalByteCount)
                }
            }
            outputStream.close()
            serverSocket.close()
        }
        Log.d(TAG, "Closed server socket")
    }

    private fun clientSends(socketAddress: InetSocketAddress) = scope.launch(Dispatchers.IO) {
        wifiP2pState = AwaitingSocketConnection
        val socket = Socket()
        socket.bind(null)
        socket.connect(socketAddress)

        wifiP2pState = SocketConnected
        val outputStream = DataOutputStream(socket.getOutputStream())
        val archivesDir = File(TrekMeContext.defaultAppDir, "archives")
        archivesDir.listFiles()?.firstOrNull()?.also {
            println("Sending ${it.name}")
            try {
                val totalByteCount = it.length()
                outputStream.writeUTF(it.name)
                outputStream.writeLong(totalByteCount)

                FileInputStream(it).use {
                    it.copyToWithProgress(outputStream, this, totalByteCount)
                }
            } catch (e: SocketException) {
                // abort
            } finally {
                outputStream.close()
                socket.close()
            }
        }
        Log.d(TAG, "Client is no longer sending")
    }

    private fun clientReceives(socketAddress: InetSocketAddress) = scope.launch(Dispatchers.IO) {
        wifiP2pState = AwaitingSocketConnection
        val socket = Socket()
        socket.bind(null)
        socket.connect(socketAddress)
        wifiP2pState = SocketConnected

        val inputStream = DataInputStream(socket.getInputStream())
        val mapName = inputStream.readUTF()
        println("Recieving $mapName from client")
        val size = inputStream.readLong()
        println("Size: $size")

        val dir = File(TrekMeContext.importedDir, mapName).unique()
        dir.mkdir()
        unzipTask(inputStream, dir, size, object : UnzipProgressionListener {
            override fun onProgress(p: Int) {
                println(p)
                wifiP2pState = Loading(p)
            }

            override fun onUnzipFinished(outputDirectory: File) {
                wifiP2pState = Loading(100)
            }

            override fun onUnzipError() {
                errorChannel.offer(WifiP2pServiceErrors.UNZIP_ERROR)
            }
        })

        inputStream.close()
        socket.close()
        Log.d(TAG, "Client is no longer receiving")
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

        wifiP2pState = Stopped
    }

    private fun InputStream.copyToWithProgress(outputStream: OutputStream, scope: CoroutineScope, totalByteCount: Long) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytes = read(buffer)
        var bytesCopied: Long = 0
        var percent = 0
        wifiP2pState = Loading(0)
        while (bytes >= 0 && scope.isActive) {
            try {
                outputStream.write(buffer, 0, bytes)
                bytes = read(buffer)
                bytesCopied += bytes
                val newPercent = (bytesCopied * 100f / totalByteCount).toInt()
                if (newPercent != percent) {
                    percent = newPercent
                    wifiP2pState = Loading(percent)
                }
            } catch (e: SocketException) {
                break
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

object Stopped : WifiP2pState() {
    override val index: Int = 10
}

enum class WifiP2pServiceErrors {
    UNZIP_ERROR
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