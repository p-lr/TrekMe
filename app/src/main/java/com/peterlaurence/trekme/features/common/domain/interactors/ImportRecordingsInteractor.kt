package com.peterlaurence.trekme.features.common.domain.interactors

import android.app.Application
import android.net.Uri
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.excursion.domain.model.Excursion
import com.peterlaurence.trekme.core.georecord.domain.model.getBoundingBox
import com.peterlaurence.trekme.core.map.domain.dao.ExcursionRefDao
import com.peterlaurence.trekme.core.map.domain.interactors.GetMapInteractor
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.intersects
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class ImportRecordingsInteractor @Inject constructor(
    private val excursionDao: ExcursionDao,
    private val excursionRefDao: ExcursionRefDao,
    private val getMapInteractor: GetMapInteractor,
    val app: Application,
    private val appEventBus: AppEventBus
) {
    suspend fun importRecordings(uriList: List<Uri>, map: Map? = null) {
        val successCnt = AtomicInteger(0)

        supervisorScope {
            uriList.forEach { uri ->
                launch {
                    val excursion = excursionDao.putExcursion(id = UUID.randomUUID().toString(), uri)
                    if (excursion != null) {
                        successCnt.incrementAndGet()
                        if (map != null) {
                            excursionRefDao.createExcursionRef(map, excursion)
                        } else {
                            /* Import in all map which intersect the recording */
                            importInAllMaps(excursion)
                        }
                    }
                }
            }
        }

        if (uriList.isNotEmpty() && uriList.size == successCnt.get()) {
            val msg =
                app.applicationContext.getString(R.string.recording_imported_success, uriList.size)
            appEventBus.postMessage(StandardMessage(msg))
        }
    }

    private suspend fun importInAllMaps(excursion: Excursion) {
        val geoRecord = excursionDao.getGeoRecord(excursion) ?: return
        val bb = geoRecord.getBoundingBox() ?: return

        supervisorScope {
            getMapInteractor.getMapList().forEach { map ->
                launch {
                    if (map.intersects(bb)) {
                        excursionRefDao.createExcursionRef(map, excursion)
                    }
                }
            }
        }
    }
}