package com.peterlaurence.trekme.repositories.gpspro

import com.peterlaurence.trekme.core.model.LocationSource
import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.di.MainDispatcher
import com.peterlaurence.trekme.ui.gpspro.events.GpsProEvents
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import javax.inject.Inject


/**
 * Performs a diagnosis of a GPS device by recording its NMEA sentences. It does so by collecting
 * one of the flows of [gpsProEvents].
 */
@ActivityRetainedScoped
class GpsProDiagnosisRepo @Inject constructor(
        private val locationSource: LocationSource,
        private val gpsProEvents: GpsProEvents,
        @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
        @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(mainDispatcher + SupervisorJob())

    private val _diagnosisRunningStateFlow = MutableStateFlow(false)
    val diagnosisRunningStateFlow = _diagnosisRunningStateFlow.asStateFlow()

    /**
     * Generates a txt report which the user is later invited to save somewhere on its device.
     * TODO: safe guard: check if a job isn't already running
     */
    fun generateReport() = scope.launch {
        _diagnosisRunningStateFlow.value = true
        /* The LocationSource requires at least one collector to work, so we're temporarily creating one*/
        val job = launch { locationSource.locationFlow.collect() }
        val sentences = getNmeaSentencesSample(10_000) // listen for 10s
        // TODO: do something with the sentences
        println("Received ${sentences.size} sentences")
        job.cancel()
        _diagnosisRunningStateFlow.value = false
    }

    private suspend fun getNmeaSentencesSample(timeout: Long): List<String> = withContext(ioDispatcher) {
        val sentences = mutableListOf<String>()
        withTimeoutOrNull(timeout) {
            gpsProEvents.nmeaSentencesFlow.collect {
                sentences.add(it)
            }
        }
        sentences
    }
}