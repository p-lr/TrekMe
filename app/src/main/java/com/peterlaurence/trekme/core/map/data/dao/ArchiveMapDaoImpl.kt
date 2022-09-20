package com.peterlaurence.trekme.core.map.data.dao

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.ArchiveMapDao
import com.peterlaurence.trekme.util.ZipProgressionListener
import com.peterlaurence.trekme.util.stackTraceAsString
import com.peterlaurence.trekme.util.zipTask
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Creates a zip file named with this [Map] name and the date. This file is placed in the
 * parent folder of the [Map].
 */
class ArchiveMapDaoImpl(
    private val fileBasedMapRegistry: FileBasedMapRegistry,
    private val app: Application,
    private val defaultDispatcher: CoroutineDispatcher
) : ArchiveMapDao {
    override suspend fun archiveMap(map: Map, listener: ZipProgressionListener, uri: Uri) {
        val docFile = DocumentFile.fromTreeUri(app.applicationContext, uri)
        if (docFile != null && docFile.isDirectory) {
            val newFileName: String = map.generateNewNameWithDate() + ".zip"
            val newFile = docFile.createFile("application/zip", newFileName)
            if (newFile != null) {
                val uriZip = newFile.uri
                runCatching {
                    val out: OutputStream = app.contentResolver.openOutputStream(uriZip)
                        ?: return
                    /* The underlying task which writes into the stream is responsible for closing this stream. */
                    withContext(defaultDispatcher) {
                        val mapFolder = fileBasedMapRegistry.getRootFolder(map.id)
                        if (mapFolder != null) {
                            zipTask(mapFolder, out, listener)
                        }
                    }
                }.onFailure {
                    Log.e(this.javaClass.name, it.stackTraceAsString())
                }.getOrNull()
            }
        }
    }

    private fun Map.generateNewNameWithDate(): String {
        val date = Date()
        val dateFormat: DateFormat = SimpleDateFormat("dd\\MM\\yyyy-HH:mm:ss", Locale.ENGLISH)
        return name + "-" + dateFormat.format(date)
    }
}