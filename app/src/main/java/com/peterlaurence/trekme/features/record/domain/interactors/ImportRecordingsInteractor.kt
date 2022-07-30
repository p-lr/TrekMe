package com.peterlaurence.trekme.features.record.domain.interactors

import android.app.Application
import android.net.Uri
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.georecord.domain.interactors.GeoRecordInteractor
import com.peterlaurence.trekme.events.AppEventBus
import com.peterlaurence.trekme.events.StandardMessage
import javax.inject.Inject

class ImportRecordingsInteractor @Inject constructor(
    private val geoRecordInteractor: GeoRecordInteractor,
    val app: Application,
    private val appEventBus: AppEventBus
) {
    suspend fun importRecordings(uriList: List<Uri>) {
        val successCnt = geoRecordInteractor.import(uriList)

        if (uriList.isNotEmpty() && uriList.size == successCnt) {
            val msg =
                app.applicationContext.getString(R.string.recording_imported_success, uriList.size)
            appEventBus.postMessage(StandardMessage(msg))
        }
    }
}