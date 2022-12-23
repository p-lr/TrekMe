package com.peterlaurence.trekme.core.location.app.producer

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationProducer
import com.peterlaurence.trekme.core.location.domain.model.LocationProducerBtInfo
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.core.lib.nmea.NmeaAggregator
import com.peterlaurence.trekme.core.lib.nmea.parseNmeaLocationSentence
import com.peterlaurence.trekme.events.gpspro.GpsProEvents
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.Executors

/**
 * A [LocationProducer] which reads NMEA sentences over bluetooth.
 * In case of connection failure, this producer tries to re-connect every 2s.
 */
class NmeaOverBluetoothProducer(
    private val bluetoothAdapter: BluetoothAdapter,
    private val connectionLostMsg: String,
    private val mode: LocationProducerBtInfo,
    private val appEventBus: AppEventBus,
    private val gpsProEvents: GpsProEvents
) : LocationProducer {

    private val connectionDispatcher by lazy {
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }
    private val readDispatcher by lazy {
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }

    override val locationFlow: Flow<Location>
        get() = makeFlow()

    private fun makeFlow(): Flow<Location> {
        return callbackFlow {
            val (socket, job) = connectAndRead()

            awaitClose {
                launch(connectionDispatcher) {
                    runCatching {
                        socket?.close()
                        job.cancel()
                    }
                }
            }
        }
    }

    private fun ProducerScope<Location>.connectAndRead(): Pair<BluetoothSocket?, Job> {
        var _socket: BluetoothSocket? = null

        val job = launch(connectionDispatcher) {
            val uuid = UUID.fromString(SPP_UUID)
            val socket = runCatching {
                bluetoothAdapter.getRemoteDevice(mode.macAddress)
                    .createRfcommSocketToServiceRecord(uuid)
            }.getOrNull() ?: return@launch
            _socket = socket

            runCatching {
                socket.connect()

                withContext(readDispatcher) {
                    val reader = BufferedReader(InputStreamReader(socket.inputStream))
                    val nmeaDataFlow = flow {
                        reader.use {
                            while (isActive) {
                                val line = runCatching<String> {
                                    reader.readLine()
                                }.getOrNull() ?: throw ConnectionLostException()

                                gpsProEvents.postNmeaSentence(line)
                                val nmeaData = parseNmeaLocationSentence(line)
                                if (nmeaData != null) {
                                    emit(nmeaData)
                                }
                            }
                        }
                    }
                    val nmeaAggregator =
                        NmeaAggregator(nmeaDataFlow) { lat, lon, speed, altitude, time ->
                            trySend(Location(lat, lon, speed, altitude, time, mode))
                        }
                    nmeaAggregator.run()
                }
            }.onFailure {
                if (it is ConnectionLostException) {
                    appEventBus.postMessage(StandardMessage(connectionLostMsg, showLong = true))
                }
                runCatching { socket.close() }
                delay(2000)
                connectAndRead()
            }
        }
        return Pair(_socket, job)
    }
}

private class ConnectionLostException : Exception()

// Serial Port Profile UUID
private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"