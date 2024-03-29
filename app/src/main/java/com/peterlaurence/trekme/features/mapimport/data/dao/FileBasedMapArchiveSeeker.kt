package com.peterlaurence.trekme.features.mapimport.data.dao

import android.app.Application
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.peterlaurence.trekme.features.mapimport.domain.dao.MapArchiveSeeker
import com.peterlaurence.trekme.features.mapimport.domain.model.MapArchive
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Leverages the Storage Access Framework (SAF) to get the list of [DocumentFile]s from a location
 * (Uri) selected by the user. The SAF guarantees that the application is granted access to that
 * location.
 */
class FileBasedMapArchiveSeeker(
    private val app: Application,
    private val ioDispatcher: CoroutineDispatcher,
    private val registry: MapArchiveRegistry
) : MapArchiveSeeker {

    override suspend fun seek(uri: Uri): List<MapArchive> = withContext(ioDispatcher) {
        val context = app.applicationContext ?: return@withContext listOf<MapArchive>()
        val docFile = DocumentFile.fromTreeUri(context, uri)
            ?: return@withContext listOf<MapArchive>()
        val docs = if (docFile.isDirectory) {
            val zipDocs = docFile.listFiles().filter {
                MimeTypeMap.getSingleton().getExtensionFromMimeType(it.type) == "zip"
            }

            zipDocs
        } else listOf()

        docs.mapNotNull { doc ->
            doc.name?.let {
                val uuid = UUID.randomUUID()
                registry.setDocumentForId(uuid, doc)
                MapArchive(uuid, it)
            }
        }
    }
}