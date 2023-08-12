package com.peterlaurence.trekme.features.record.domain.interactors

import android.app.Application
import android.net.Uri
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.excursion.domain.dao.ExcursionDao
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

class ImportRecordingsInteractor @Inject constructor(
    private val excursionDao: ExcursionDao,
    val app: Application,
    private val appEventBus: AppEventBus
) {
    suspend fun importRecordings(uriList: List<Uri>) {
        val successCnt = AtomicInteger(0)
        coroutineScope {
            uriList.forEach { uri ->
                launch {
                    val success = excursionDao.putExcursion(id = UUID.randomUUID().toString(), uri)
                    if (success) successCnt.incrementAndGet()
                }
            }
        }

        if (uriList.isNotEmpty() && uriList.size == successCnt.get()) {
            val msg =
                app.applicationContext.getString(R.string.recording_imported_success, uriList.size)
            appEventBus.postMessage(StandardMessage(msg))
        }
    }
}