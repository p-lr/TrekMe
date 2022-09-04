package com.peterlaurence.trekme.features.mapcreate.domain.interactors

import android.content.ContentResolver
import android.net.Uri
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import javax.inject.Inject

class ParseGeoRecordInteractor @Inject constructor(
    private val geoRecordParser: GeoRecordParser
) {
    suspend fun parseGeoRecord(uri: Uri, contentResolver: ContentResolver): GeoRecord? {
        return geoRecordParser.parse(uri, contentResolver)
    }
}