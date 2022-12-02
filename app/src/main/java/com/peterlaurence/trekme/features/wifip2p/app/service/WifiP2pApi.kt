package com.peterlaurence.trekme.features.wifip2p.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


suspend fun WifiP2pManager.addLocalService(
    context: Context,
    c: WifiP2pManager.Channel,
    servInfo: WifiP2pServiceInfo
): Boolean = suspendCoroutine { cont ->
    val listener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            cont.resume(true)
        }

        override fun onFailure(reason: Int) {
            if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                cont.resumeWithException(IllegalStateException())
            } else cont.resume(false)
        }
    }

    if (ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        addLocalService(c, servInfo, listener)
    } else {
        cont.resume(false)
    }
}

suspend fun WifiP2pManager.clearLocalServices(c: WifiP2pManager.Channel?): Boolean =
    suspendCoroutine { cont ->
        if (c == null) {
            return@suspendCoroutine
        }
        val listener = object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                cont.resume(true)
            }

            override fun onFailure(reason: Int) {
                cont.resume(false)
            }
        }
        clearLocalServices(c, listener)
    }

suspend fun WifiP2pManager.clearServiceRequests(c: WifiP2pManager.Channel?): Boolean =
    suspendCoroutine { cont ->
        if (c == null) {
            return@suspendCoroutine
        }
        val listener = object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                cont.resume(true)
            }

            override fun onFailure(reason: Int) {
                cont.resume(false)
            }
        }
        clearServiceRequests(c, listener)
    }

suspend fun WifiP2pManager.cancelConnect(c: WifiP2pManager.Channel): Boolean =
    suspendCoroutine { cont ->
        val listener = object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                cont.resume(true)
            }

            override fun onFailure(reason: Int) {
                cont.resume(false)
            }
        }
        cancelConnect(c, listener)
    }

suspend fun WifiP2pManager.removeGroup(c: WifiP2pManager.Channel): Boolean =
    suspendCoroutine { cont ->
        val listener = object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                cont.resume(true)
            }

            override fun onFailure(reason: Int) {
                cont.resume(false)
            }
        }
        removeGroup(c, listener)
    }

suspend fun WifiP2pManager.stopPeerDiscovery(c: WifiP2pManager.Channel?): Boolean {
    return withTimeoutOrNull(2000) {
        suspendCancellableCoroutine { cont ->
            if (c == null) {
                return@suspendCancellableCoroutine
            }
            val listener = object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    cont.resume(true)
                }

                override fun onFailure(reason: Int) {
                    cont.resume(false)
                }
            }
            stopPeerDiscovery(c, listener)
        }
    } ?: false
}

suspend fun WifiP2pManager.discoverPeers(context: Context, c: WifiP2pManager.Channel?): Boolean =
    suspendCoroutine { cont ->
        if (c == null) {
            return@suspendCoroutine
        }
        val listener = object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                cont.resume(true)
            }

            override fun onFailure(reason: Int) {
                if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                    cont.resumeWithException(IllegalStateException())
                } else cont.resume(false)
            }
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            discoverPeers(c, listener)
        } else {
            cont.resume(false)
        }
    }

suspend fun WifiP2pManager.addServiceRequest(
    c: WifiP2pManager.Channel,
    req: WifiP2pServiceRequest
): Boolean = suspendCoroutine { cont ->
    val listener = object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
            cont.resume(true)
        }

        override fun onFailure(reason: Int) {
            cont.resume(false)
        }
    }
    addServiceRequest(c, req, listener)
}

suspend fun WifiP2pManager.discoverServices(context: Context, c: WifiP2pManager.Channel): Boolean =
    suspendCoroutine { cont ->
        val listener = object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                cont.resume(true)
            }

            override fun onFailure(reason: Int) {
                if (reason == WifiP2pManager.P2P_UNSUPPORTED) {
                    cont.resumeWithException(IllegalStateException())
                } else cont.resume(false)
            }
        }
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            discoverServices(c, listener)
        } else {
            cont.resume(false)
        }
    }

suspend fun WifiP2pManager.connect(context: Context, c: WifiP2pManager.Channel, conf: WifiP2pConfig): Boolean =
    suspendCoroutine { cont ->
        val listener = object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                cont.resume(true)
            }

            override fun onFailure(reason: Int) {
                cont.resume(false)
            }
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            connect(c, conf, listener)
        } else {
            cont.resume(false)
        }
    }