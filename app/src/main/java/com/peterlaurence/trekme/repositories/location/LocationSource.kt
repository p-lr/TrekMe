package com.peterlaurence.trekme.repositories.location

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

/**
 * This [LocationSource] uses two [LocationProducer]s. One called "internal" corresponds to
 * the device's GPS antenna. The other called "external" corresponds to whatever external bluetooth
 * or wifi connected GPS.
 */
class LocationSourceImpl(
        mode: Flow<LocationProducerInfo>,
        private val internalProducer: LocationProducer,
        private val externalProducer: LocationProducer
) : LocationSource {
    /**
     * A [SharedFlow] of [Location]s, with a replay of 1.
     * Automatically un-registers underlying callback when there are no collectors.
     * N.B: This shared flow used to be conflated, using a trick reported in
     * https://github.com/Kotlin/kotlinx.coroutines/issues/2408 is fixed
     */
    override val locationFlow: SharedFlow<Location> by lazy {
        callbackFlow {
            val producer = ProducersController(mode, internalProducer, externalProducer)
            producer.locationFlow.map {
                trySend(it)
            }.launchIn(this)

            awaitClose {
                producer.stop()
            }
        }.shareIn(
                ProcessLifecycleOwner.get().lifecycleScope,
                SharingStarted.WhileSubscribed(),
                1
        )
    }
}

private class ProducersController(
        state: Flow<LocationProducerInfo>,
        private val internalProducer: LocationProducer,
        private val externalProducer: LocationProducer
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var internalJob: Job? = null
    private var externalJob: Job? = null

    val _locationFlow = MutableSharedFlow<Location>(
            extraBufferCapacity = 256,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val locationFlow: SharedFlow<Location> = _locationFlow.asSharedFlow()

    init {
        state.map { mode ->
            when (mode) {
                InternalGps -> startInternal()
                is LocationProducerBtInfo -> startExternal()
            }
        }.launchIn(scope)
    }

    fun stop() = scope.cancel()

    private fun startInternal() {
        externalJob?.run { cancel() }
        internalJob = collectProducer(internalProducer)
    }

    private fun startExternal() {
        internalJob?.run { cancel() }
        externalJob = collectProducer(externalProducer)
    }

    private fun collectProducer(producer: LocationProducer): Job {
        return producer.locationFlow.map {
            _locationFlow.tryEmit(it)
        }.launchIn(scope)
    }
}