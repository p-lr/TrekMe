package com.peterlaurence.trekme.features.common.domain.repositories

import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.track.TrackTools
import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.core.georecord.domain.repository.GeoRecordRepository
import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Associates each recordings with [RecordingData]s, which contain basic properties and
 * track statistics. Those statistics come from dynamically computed [GeoRecord] instances. However,
 * [GeoRecord] objects are potentially heavy. This is the reason why we don't store them, and use
 * lightweight [RecordingData].
 * Also, each [RecordingData] has the same id as the corresponding [GeoRecord].
 *
 * @since 2022/07/17
 */
@Singleton
class RecordingDataRepository @Inject constructor(
    private val geoRecordRepository: GeoRecordRepository,
    @IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val primaryScope = ProcessLifecycleOwner.get().lifecycleScope

    val recordingDataFlow = MutableStateFlow<List<RecordingData>>(emptyList())

    init {
        primaryScope.launch {
            geoRecordRepository.getGeoRecordsFlow().collect { geoRecordList ->
                val recordingDataList = geoRecordList.map { geoRecord ->
                    val existingRecordingData = recordingDataFlow.value.firstOrNull { recordingData ->
                        recordingData.id == geoRecord.id
                    }
                    val recordingData = existingRecordingData ?: makeRecordingData(geoRecord)
                    recordingData.copy(name = geoRecord.name)
                }

                recordingDataFlow.value = recordingDataList.mostRecentFirst()
            }
        }
    }

    private fun List<RecordingData>.mostRecentFirst() = sortedByDescending {
        it.time ?: -1
    }

    private suspend fun makeRecordingData(geoRecord: GeoRecord): RecordingData {
        return withContext(ioDispatcher) {
            val routeIds: List<String> = geoRecord.routeGroups.flatMap { it.routes }.map { it.id }
            val statistics = geoRecord.let {
                TrackTools.getGeoStatistics(it)
            }

            RecordingData(
                geoRecord.id,
                geoRecord.name,
                statistics,
                routeIds,
                geoRecord.time
            )
        }
    }
}
