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

    private var diagnosisJob: Job? = null

    /**
     * Generates a txt report which the user is later invited to save somewhere on its device.
     * The "diagnosis" file content is just the concatenation of the device name and all NMEA
     * sentences.
     */
    fun generateDiagnosis(deviceName: String) {
        if (diagnosisJob?.isActive == true) return

        diagnosisJob = scope.launch {
            _diagnosisRunningStateFlow.value = DiagnosisRunning

            /* The LocationSource requires at least one collector to work, so we're temporarily creating one*/
            val job = launch { locationSource.locationFlow.collect() }
            val sentences = getNmeaSentencesSample(10_000) // listen for 10s
            job.cancel()

            /* Recording done, now update the state */
            _diagnosisRunningStateFlow.value = if (sentences.isNotEmpty()) {
                val fileContent = "${deviceName}\n" + sentences.joinToString("\n")
                DiagnosisAwaitingSave(sentences.size, fileContent)
            } else DiagnosisEmpty
        }
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

    fun saveDiagnosis() {
        val state = _diagnosisRunningStateFlow.value
        if (state is DiagnosisAwaitingSave) {
            gpsProEvents.writeDiagnosisFile(state.fileContent)
        }

        /* Whatever the outcome, go back to ready state */
        _diagnosisRunningStateFlow.value = Ready
    }
}

sealed interface DiagnosisState
object Ready : DiagnosisState
object DiagnosisRunning : DiagnosisState
object DiagnosisEmpty : DiagnosisState
data class DiagnosisAwaitingSave(val nbSentences: Int, val fileContent: String) : DiagnosisState