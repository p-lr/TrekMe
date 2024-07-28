package com.peterlaurence.trekme.core.location.domain.repository

import com.peterlaurence.trekme.core.location.domain.model.*
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
    modeFlow: Flow<LocationProducerInfo>,
    flowSelector: (LocationProducerInfo) -> Flow<Location>,
    processScope: CoroutineScope
) : LocationSource {
    /**
     * A [SharedFlow] of [Location]s, with a replay of 1.
     * Automatically un-registers underlying callback when there are no collectors.
     * N.B: This shared flow used to be conflated, using a trick reported in
     * https://github.com/Kotlin/kotlinx.coroutines/issues/2408. However, we don't want slow
     * collectors (especially recorders) to miss locations.
     */
    override val locationFlow: SharedFlow<Location> by lazy {
        callbackFlow {
            val producer = ProducersController(modeFlow, flowSelector)
            producer.locationFlow.map {
                trySend(it)
            }.launchIn(this)

            awaitClose {
                producer.stop()
            }
        }.shareIn(
            processScope,
            SharingStarted.WhileSubscribed(),
            1
        )
    }
}

private class ProducersController(
    state: Flow<LocationProducerInfo>,
    private val flowSelector: (LocationProducerInfo) -> Flow<Location>
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var internalJob: Job? = null
    private var externalJob: Job? = null

    private val _locationFlow = MutableSharedFlow<Location>(
        extraBufferCapacity = 256,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val locationFlow: SharedFlow<Location> = _locationFlow.asSharedFlow()

    init {
        state.distinctUntilChanged().map { mode ->
            val flow = flowSelector(mode)
            when (mode) {
                InternalGps -> startInternal(flow)
                is LocationProducerBtInfo -> startExternal(flow)
            }
        }.launchIn(scope)
    }

    fun stop() = scope.cancel()

    private fun startInternal(flow: Flow<Location>) {
        externalJob?.run { cancel() }
        internalJob = collectProducer(flow)
    }

    private fun startExternal(flow: Flow<Location>) {
        internalJob?.run { cancel() }
        externalJob = collectProducer(flow)
    }

    private fun collectProducer(locationFlow: Flow<Location>): Job {
        return locationFlow.map {
            _locationFlow.tryEmit(it)
        }.launchIn(scope)
    }
}