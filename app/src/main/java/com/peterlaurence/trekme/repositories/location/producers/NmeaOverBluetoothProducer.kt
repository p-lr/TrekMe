package com.peterlaurence.trekme.repositories.location.producers

import android.bluetooth.BluetoothAdapter
import com.peterlaurence.trekme.core.model.Location
import com.peterlaurence.trekme.core.model.LocationProducer
import com.peterlaurence.trekme.lib.nmea.NmeaAggregator
import com.peterlaurence.trekme.lib.nmea.parseNmeaLocationSentence
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.Executors

/**
 * A [LocationProducer] which reads NMEA sentences over bluetooth.
 */
class NmeaOverBluetoothProducer(private val macAddress: String) : LocationProducer {
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
            val socket = withContext(connectionDispatcher) {
                val uuid = UUID.fromString(SPP_UUID)
                BluetoothAdapter.getDefaultAdapter().getRemoteDevice(macAddress).createRfcommSocketToServiceRecord(uuid)
            }

            val job = launch(connectionDispatcher) {
                socket.connect()

                withContext(readDispatcher) {
                    val reader = BufferedReader(InputStreamReader(socket.inputStream))
                    val nmeaDataFlow = flow {
                        reader.use {
                            while (isActive) {
                                val nmeaData = parseNmeaLocationSentence(reader.readLine())
                                if (nmeaData != null) {
                                    emit(nmeaData)
                                }
                            }
                        }
                    }
                    val nmeaAggregator = NmeaAggregator(nmeaDataFlow) {
                        trySend(it)
                    }
                    nmeaAggregator.run()
                }
            }
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
}

// Serial Port Profile UUID
private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"