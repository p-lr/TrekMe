package com.peterlaurence.trekme.core.georecord.data.dao

import android.content.ContentResolver
import android.net.Uri
import com.peterlaurence.trekme.core.georecord.data.mapper.gpxToDomain
import com.peterlaurence.trekme.core.georecord.domain.dao.GeoRecordParser
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.lib.gpx.parseGpxSafely
import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.readUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoRecordParserImpl @Inject constructor(
    @IoDispatcher
    private val dispatcher: CoroutineDispatcher
): GeoRecordParser {
    private val defaultRecordingName = "A track"

    override suspend fun parse(
        uri: Uri, contentResolver: ContentResolver
    ): GeoRecord? {
        return runCatching {
            withContext(dispatcher) {
                readUri(uri, contentResolver) {
                    val trackName = FileUtils.getFileRealFileNameFromURI(contentResolver, uri)
                        ?: defaultRecordingName
                    parse(it, trackName)
                }
            }
        }.getOrNull()
    }

    override suspend fun parse(inputStream: InputStream, defaultName: String): GeoRecord? {
        return parseGpxSafely(inputStream)?.let { gpx ->
            gpxToDomain(gpx, defaultName)
        }
    }
}