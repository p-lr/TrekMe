package com.peterlaurence.trekme.features.record.domain.interactors

import android.app.Application
import android.net.Uri
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import com.peterlaurence.trekme.features.common.domain.repositories.GeoRecordRepository
import javax.inject.Inject

class ImportRecordingsInteractor @Inject constructor(
    private val geoRecordRepository: GeoRecordRepository,
    val app: Application,
    private val appEventBus: AppEventBus
) {
    suspend fun importRecordings(uriList: List<Uri>) {
        var successCnt = 0
        uriList.forEach { uri ->
            if (geoRecordRepository.importRecordingFromUri(uri)) successCnt++
        }
        if (uriList.isNotEmpty() && uriList.size == successCnt) {
            val msg =
                app.applicationContext.getString(R.string.recording_imported_success, uriList.size)
            appEventBus.postMessage(StandardMessage(msg))
        }
    }
}