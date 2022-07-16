package com.peterlaurence.trekme.core.georecord.data.dao

import android.content.ContentResolver
import android.net.Uri
import com.peterlaurence.trekme.core.georecord.data.convertGpx
import com.peterlaurence.trekme.core.georecord.domain.interactors.GeoRecordDao
import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.lib.gpx.parseGpxSafely
import com.peterlaurence.trekme.di.IoDispatcher
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.readUri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoRecordDaoImpl @Inject constructor(
    @IoDispatcher
    private val dispatcher: CoroutineDispatcher
): GeoRecordDao {
    override suspend fun parseGpx(
        uri: Uri, contentResolver: ContentResolver
    ): GeoRecord? {
        return withContext(dispatcher) {
            readUri(uri, contentResolver) {
                val trackName = FileUtils.getFileRealFileNameFromURI(contentResolver, uri)
                    ?: "A track"
                parseGpx(it, trackName)
            }
        }
    }

    override suspend fun parseGpx(inputStream: InputStream, defaultTrackName: String): GeoRecord? {
        return parseGpxSafely(inputStream)?.let { gpx ->
            convertGpx(gpx, defaultTrackName)
        }
    }
}