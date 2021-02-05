package com.peterlaurence.trekme.repositories.recording

import android.content.ContentResolver
import android.net.Uri
import com.peterlaurence.trekme.core.TrekMeContext
import com.peterlaurence.trekme.util.FileUtils
import com.peterlaurence.trekme.util.gpx.model.Gpx
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GpxRepository @Inject constructor(private val trekMeContext: TrekMeContext) {
    private val _gpxForElevation = MutableSharedFlow<GpxForElevation?>(
            1, 0, BufferOverflow.DROP_OLDEST)

    private val supportedTrackFilesExtensions = arrayOf("gpx", "xml")

    private val supportedFileFilter = filter@{ dir: File, filename: String ->
        /* We only look at files */
        if (File(dir, filename).isDirectory) {
            return@filter false
        }

        supportedTrackFilesExtensions.any { filename.endsWith(".$it") }
    }

    /**
     * Get the list of [File] which extension is in the list of supported extension for track
     * file. Files are searched into the [TrekMeContext.recordingsDir].
     */
    val recordings: Array<File>?
        get() = trekMeContext.recordingsDir?.listFiles(supportedFileFilter)

    val gpxForElevation = _gpxForElevation.asSharedFlow()

    fun setGpxForElevation(gpx: Gpx, gpxFile: File) {
        _gpxForElevation.tryEmit(GpxForElevation(gpx, gpxFile))
    }

    fun resetGpxForElevation() {
        _gpxForElevation.tryEmit(null)
    }

    fun isFileSupported(uri: Uri, contentResolver: ContentResolver): Boolean {
        val fileName = FileUtils.getFileRealFileNameFromURI(contentResolver, uri)
        val extension = fileName.substringAfterLast('.', "")

        if ("" == extension) return false

        return supportedTrackFilesExtensions.any { it == extension }
    }
}

/**
 * Contains a [Gpx] along with a unique [id].
 */
data class GpxForElevation(val gpx: Gpx, val gpxFile: File) {
    val id = gpxFile.hashCode()
}