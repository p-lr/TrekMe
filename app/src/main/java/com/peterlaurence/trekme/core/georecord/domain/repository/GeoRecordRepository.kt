package com.peterlaurence.trekme.core.georecord.domain.repository

import android.net.Uri
import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordDao
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecordLightWeight
import com.peterlaurence.trekme.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Since [GeoRecord]s are potentially heavy objects, this repository only exposes a [StateFlow] of
 * [GeoRecordLightWeight].
 * However, it's still possible to get a single [GeoRecord] using [getGeoRecord].
 *
 * Since excursions don't have a dedicated ui yet, they appear as regular recordings for the moment.
 * Consequently, this repository supports two kinds of [GeoRecordLightWeight]:
 * - the ones that refer to regular [GeoRecord]
 * - the ones that refer to excursions
 * Existing regular recordings will be eventually converted to excursions, so that excursions will
 * be the only supported type.
 *
 * @since 2022/07/30
 */
@Singleton
class GeoRecordRepository @Inject constructor(
    private val geoRecordDao: GeoRecordDao,
    private val excursionDao: ExcursionDao,
    @ApplicationScope
    private val applicationScope: CoroutineScope
) {
    private val mappedGeoRecordsFlow = MutableStateFlow<List<GeoRecordLightWeight>>(emptyList())
    private val rosetta = mutableMapOf<String, UUID>()
    private val reversedRosetta: Map<UUID, String>
        get() = rosetta.entries.associate { (k, v) -> v to k }

    init {
        applicationScope.launch {
            excursionDao.getExcursionsFlow().collect { excursions ->
                val mappedGeoRecords = excursions.map { exc ->
                    val uuid = rosetta[exc.id] ?: run {
                        UUID.randomUUID().also {
                            rosetta[exc.id] = it
                        }
                    }
                    GeoRecordLightWeight(uuid, name = exc.title)
                }
                mappedGeoRecordsFlow.value = mappedGeoRecords
            }
        }
    }

    /* For the moment, the repository only exposes the flow from the file-based source */
    fun getGeoRecordsFlow(): StateFlow<List<GeoRecordLightWeight>> {
        val geoRecordFlow = geoRecordDao.getGeoRecordsFlow()
        return combine(geoRecordFlow, mappedGeoRecordsFlow) { x, y ->
            x + y
        }.stateIn(applicationScope, SharingStarted.Eagerly, geoRecordFlow.value)
    }

    fun getUri(id: UUID): Uri? {
        val excursionId = rosetta.entries.firstNotNullOfOrNull {
            if (it.value == id) it.key else null
        }
        return if (excursionId != null) {
            excursionDao.getGeoRecordUri(excursionId)
        } else {
            geoRecordDao.getUri(id)
        }
    }

    suspend fun getGeoRecord(id: UUID): GeoRecord? {
        val excursionId = reversedRosetta[id]
        return if (excursionId != null) {
            excursionDao.getExcursionsFlow().value.firstNotNullOfOrNull {
                if (it.id == excursionId) {
                    /* Purposely set the georecord uuid to the corresponding georecordlightweight uuid */
                    excursionDao.getGeoRecord(it)?.copy(id = id, name = it.title)
                } else null
            }
        } else {
            geoRecordDao.getRecord(id)
        }
    }

    suspend fun renameGeoRecord(id: UUID, newName: String): Boolean {
        val excursionId = rosetta.entries.firstNotNullOfOrNull {
            if (it.value == id) it.key else null
        }
        return if (excursionId != null) {
            excursionDao.rename(excursionId, newName)
        } else {
            geoRecordDao.renameGeoRecord(id, newName)
        }
    }

    suspend fun updateGeoRecord(geoRecord: GeoRecord) {
        val excursionId = rosetta.entries.firstNotNullOfOrNull {
            if (it.value == geoRecord.id) it.key else null
        }

        if (excursionId != null) {
            excursionDao.updateGeoRecord(excursionId, geoRecord)
        } else {
            geoRecordDao.updateGeoRecord(geoRecord)
        }
    }

    suspend fun deleteGeoRecords(ids: List<UUID>): Boolean {
        val excursionIds = rosetta.entries.mapNotNull {
            if (it.value in ids) it.key else null
        }

        return supervisorScope {
            val job1 = async {
                excursionDao.deleteExcursions(excursionIds)
            }
            val job2 = async {
                geoRecordDao.deleteGeoRecords(ids)
            }
            job1.await() && job2.await()
        }
    }

    fun getExcursionId(id: UUID): String? {
        return rosetta.entries.firstNotNullOfOrNull {
            if (it.value == id) it.key else null
        }
    }

    fun getExcursionIds(ids: List<UUID>): List<String> {
        return rosetta.entries.mapNotNull {
            if (it.value in ids) it.key else null
        }
    }
}