package com.peterlaurence.trekme.repositories.gpspro

import com.peterlaurence.trekme.core.model.LocationSource
import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.di.MainDispatcher
import com.peterlaurence.trekme.ui.gpspro.events.GpsProEvents
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _diagnosisRunningStateFlow = MutableStateFlow<DiagnosisState>(Ready)
    val diagnosisRunningStateFlow: StateFlow<DiagnosisState> = _diagnosisRunningStateFlow.asStateFlow()

    /**
     * Generates a txt report which the user is later invited to save somewhere on its device.
     * TODO: safe guard: check if a job isn't already running
     */
    fun generateDiagnosis() = scope.launch {
        _diagnosisRunningStateFlow.value = DiagnosisRunning
        /* The LocationSource requires at least one collector to work, so we're temporarily creating one*/
        val job = launch { locationSource.locationFlow.collect() }
        val sentences = getNmeaSentencesSample(10_000) // listen for 10s
        // TODO: do something with the sentences
        println("Received ${sentences.size} sentences")
        job.cancel()
        _diagnosisRunningStateFlow.value = if(sentences.isNotEmpty()) {
            DiagnosisAwaitingSave(sentences.size)
        } else DiagnosisEmpty
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

    fun cancelDiagnosis() {
        _diagnosisRunningStateFlow.value = Ready
    }
}

sealed interface DiagnosisState
object Ready : DiagnosisState
object DiagnosisRunning : DiagnosisState
object DiagnosisEmpty : DiagnosisState
data class DiagnosisAwaitingSave(val nbSentences: Int) : DiagnosisState