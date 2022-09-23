package com.peterlaurence.trekme.features.mapimport.data.dao

import android.app.Application
import com.peterlaurence.trekme.core.map.data.MAP_IMPORTED_FOLDER_NAME
import com.peterlaurence.trekme.core.map.data.utils.unarchive
import com.peterlaurence.trekme.core.map.domain.dao.MapSeekerDao
import com.peterlaurence.trekme.core.map.domain.models.MapImportResult
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.features.mapimport.domain.dao.MapArchiveRegistry
import com.peterlaurence.trekme.features.mapimport.domain.dao.UnarchiveDao
import com.peterlaurence.trekme.features.mapimport.domain.model.*
import com.peterlaurence.trekme.util.UnzipProgressionListener
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

/**
 * A file based implementation, which:
 * 1. Extracts the archive into a predefined folder
 * 2. Imports the map from the extracted folder
 */
class UnarchiveDaoImpl(
    private val app: Application,
    private val ioDispatcher: CoroutineDispatcher,
    private val settings: Settings,
    private val registry: MapArchiveRegistry,
    private val mapRepository: MapRepository,
    private val mapSeekerDao: MapSeekerDao
) : UnarchiveDao {
    override suspend fun unarchive(id: UUID): Flow<UnzipEvent> {
        return withContext(ioDispatcher) {
            val document = registry.getDocumentForId(id) ?: return@withContext emptyFlow()
            val name = document.name ?: return@withContext emptyFlow()

            val inputStream =
                app.contentResolver.openInputStream(document.uri) ?: return@withContext emptyFlow()
            val rootFolder = settings.getAppDir().firstOrNull() ?: return@withContext emptyFlow()
            val outputFolder = File(rootFolder, MAP_IMPORTED_FOLDER_NAME)

            callbackFlow {
                val producerScope = this
                launch {
                    unarchive(inputStream,
                        outputFolder,
                        name,
                        document.length(),
                        object : UnzipProgressionListener {
                            override fun onProgress(p: Int) {
                                trySend(UnzipProgressEvent(id, p))
                            }

                            /* Import the extracted map */
                            override fun onUnzipFinished(outputDirectory: File, percent: Double) {
                                producerScope.launch {
                                    send(UnzipFinishedEvent(id))
                                    importMapFromFolder(outputDirectory).also { result ->
                                        send(UnzipMapImportedEvent(id, result.map, result.status))
                                    }
                                }
                            }

                            override fun onUnzipError() {
                                producerScope.launch {
                                    send(UnzipErrorEvent(id))
                                }
                            }
                        }
                    )
                }
                awaitClose {}
            }
        }
    }

    private suspend fun importMapFromFolder(folder: File) = withContext(ioDispatcher) {
        val map = mapSeekerDao.seek(folder)
        if (map != null) {
            mapRepository.addMaps(listOf(map))
        }

        MapImportResult(map, mapSeekerDao.status)
    }
}