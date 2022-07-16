package com.peterlaurence.trekme.core.georecord.domain.interactors

import android.content.ContentResolver
import android.net.Uri
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import java.io.InputStream

interface GeoRecordDao {
    suspend fun parseGpx(uri: Uri, contentResolver: ContentResolver): GeoRecord?
    suspend fun parseGpx(inputStream: InputStream, defaultTrackName: String): GeoRecord?
}