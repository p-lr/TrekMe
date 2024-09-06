package com.peterlaurence.trekme.features.record.domain.repositories

import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.logic.getGeoStatistics
import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.di.ApplicationScope
import com.peterlaurence.trekme.features.common.domain.model.Loading
import com.peterlaurence.trekme.features.common.domain.model.RecordingDataStateOwner
import com.peterlaurence.trekme.features.common.domain.model.RecordingsAvailable
import com.peterlaurence.trekme.features.common.domain.model.RecordingsState
import com.peterlaurence.trekme.features.record.domain.model.RecordingData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Associates each recordings with [RecordingData]s, which contain basic properties and track
 * statistics. Recordings are only excursions for the moment.
 * For each [Excursion] corresponds a [GeoRecord] instance. Those statistics come from dynamically
 * computed [GeoRecord] instances. However, [GeoRecord] objects are potentially heavy. This is the
 * reason why we don't store them, and use lightweight [RecordingData].
 * Also, each [RecordingData] has the same id as the corresponding [Excursion].
 *
 * @since 2022/07/17
 */
@Singleton
class RecordingDataRepository @Inject constructor(
    private val excursionDao: ExcursionDao,
    @IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
    @ApplicationScope
    processScope: CoroutineScope,
) : RecordingDataStateOwner {

    override val recordingDataFlow = MutableStateFlow<RecordingsState>(Loading)

    init {
        processScope.launch {
            /**
             * When a [RecordingData] already exists, just update the name. Otherwise, make new
             * [RecordingData]s by requesting full [GeoRecord]s.
             */
            excursionDao.migrateLegacyRecordingsToExcursions()
            excursionDao.getExcursionsFlow().collect { excursions ->
                val newExcursions = mutableListOf<Excursion>()
                val recordingDataList = excursions.mapNotNull {
                    val recordingsState = recordingDataFlow.value
                    val existingRecordingData = if (recordingsState is RecordingsAvailable) {
                        recordingsState.recordings.firstOrNull { recordingData ->
                            recordingData.id == it.id
                        }
                    } else null
                    if (existingRecordingData == null) {
                        newExcursions.add(it)
                    }
                    /* The id is the same, but the name might have changed */
                    existingRecordingData?.copy(name = it.title)
                }

                val newRecordings = makeRecordingData(newExcursions)

                recordingDataFlow.value = RecordingsAvailable((recordingDataList + newRecordings).mostRecentFirst())
            }
        }
    }

    private fun List<RecordingData>.mostRecentFirst() = sortedByDescending {
        it.time ?: -1
    }

    private suspend fun makeRecordingData(excursions: List<Excursion>): List<RecordingData> {
        val concurrency = (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(2)

        return excursions.asFlow().map {  excursion ->
            flow {
                val geoRecord = makeRecordingData(excursion)
                if (geoRecord != null) {
                    emit(geoRecord)
                }
            }
        }.flowOn(ioDispatcher).flattenMerge(concurrency).toList()
    }

    private suspend fun makeRecordingData(excursion: Excursion): RecordingData? {
        val geoRecord = excursionDao.getGeoRecord(excursion) ?: return null
        return withContext(ioDispatcher) {
            val routeIds: List<String> = geoRecord.routeGroups.flatMap { it.routes }.map { it.id }
            val statistics = getGeoStatistics(geoRecord)

            RecordingData(
                excursion.id,
                excursion.title,
                statistics,
                routeIds,
                geoRecord.time
            )
        }
    }
}
