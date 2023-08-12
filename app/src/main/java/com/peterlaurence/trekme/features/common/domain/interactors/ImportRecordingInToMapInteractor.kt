package com.peterlaurence.trekme.features.common.domain.interactors

import android.app.Application
import android.net.Uri
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class ImportRecordingInToMapInteractor @Inject constructor(
    private val excursionDao: ExcursionDao,
    val app: Application,
    private val appEventBus: AppEventBus
) {
    suspend fun importRecordingIntoMap(uri: Uri, map: Map) {
        val success = excursionDao.putExcursion(id = UUID.randomUUID().toString(), uri)

        if (success) {
            val msg =
                app.applicationContext.getString(R.string.recording_imported_success, 1)
            appEventBus.postMessage(StandardMessage(msg))
        }
    }
}