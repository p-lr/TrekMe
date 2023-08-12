package com.peterlaurence.trekme.core.georecord.domain.dao

import android.content.ContentResolver
import android.net.Uri
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import java.io.File
import java.io.InputStream

interface GeoRecordParser {
    suspend fun parse(uri: Uri, contentResolver: ContentResolver): GeoRecord?
    suspend fun parse(inputStream: InputStream, defaultName: String): GeoRecord?
}