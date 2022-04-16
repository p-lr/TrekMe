package com.peterlaurence.trekme.core.map.data.dao

import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.dao.ArchiveMapDao
import com.peterlaurence.trekme.util.ZipProgressionListener
import com.peterlaurence.trekme.util.zipTask
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.OutputStream

class ArchiveMapDaoImpl(
    private val defaultDispatcher: CoroutineDispatcher
): ArchiveMapDao {
    override suspend fun archiveMap(map: Map, listener: ZipProgressionListener, outputStream: OutputStream) {
        withContext(defaultDispatcher) {
            val mapFolder = map.configFile.parentFile
            if (mapFolder != null) {
                zipTask(mapFolder, outputStream, listener)
            }
        }
    }
}