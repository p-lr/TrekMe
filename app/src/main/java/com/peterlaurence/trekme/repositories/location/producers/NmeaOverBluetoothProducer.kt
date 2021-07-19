package com.peterlaurence.trekme.repositories.location.producers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import com.peterlaurence.trekme.core.events.AppEventBus
import com.peterlaurence.trekme.core.events.StandardMessage
import com.peterlaurence.trekme.core.model.Location
import com.peterlaurence.trekme.core.model.LocationProducer
import com.peterlaurence.trekme.core.model.LocationProducerBtInfo
import com.peterlaurence.trekme.lib.nmea.NmeaAggregator
import com.peterlaurence.trekme.lib.nmea.parseNmeaLocationSentence
import com.peterlaurence.trekme.ui.gpspro.events.GpsProEvents
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
                        socket.close()
                        job.cancel()
                    }
                }
            }
        }
    }

    private suspend fun ProducerScope<Location>.connectAndRead(): Pair<BluetoothSocket, Job> {
        val socket = withContext(connectionDispatcher) {
            val uuid = UUID.fromString(SPP_UUID)
            BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mode.macAddress).createRfcommSocketToServiceRecord(uuid)
        }

        val job = launch(connectionDispatcher) {
            runCatching {
                socket.connect()

                withContext(readDispatcher) {
                    val reader = BufferedReader(InputStreamReader(socket.inputStream))
                    val nmeaDataFlow = flow {
                        reader.use {
                            while (isActive) {
                                val line = runCatching {
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
                    val nmeaAggregator = NmeaAggregator(nmeaDataFlow) { lat, lon, speed, altitude, time ->
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
        return Pair(socket, job)
    }
}

private class ConnectionLostException : Exception()

// Serial Port Profile UUID
private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"