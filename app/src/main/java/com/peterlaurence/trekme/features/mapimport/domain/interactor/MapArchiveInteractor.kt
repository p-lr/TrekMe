package com.peterlaurence.trekme.features.mapimport.domain.interactor

import androidx.documentfile.provider.DocumentFile
import com.peterlaurence.trekme.features.mapimport.domain.dao.MapArchiveRegistry
import com.peterlaurence.trekme.features.mapimport.domain.dao.UnarchiveDao
import com.peterlaurence.trekme.features.mapimport.domain.model.MapArchive
import com.peterlaurence.trekme.features.mapimport.domain.model.UnzipEvent
import com.peterlaurence.trekme.features.mapimport.domain.repository.MapArchiveRepository
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject

class MapArchiveInteractor @Inject constructor(
    private val repository: MapArchiveRepository,
    private val registry: MapArchiveRegistry,
    private val unarchiveDao: UnarchiveDao,
) {
    fun setArchives(docs: List<DocumentFile>) {
        val archives = docs.mapNotNull { doc ->
            doc.name?.let {
                val uuid = UUID.randomUUID()
                registry.setDocumentForId(uuid, doc)
                MapArchive(uuid, it)
            }
        }
        repository.setArchives(archives)
    }

    /**
     * Launch the decompression of an archive.
     */
    suspend fun unarchiveAndGetEvents(archive: MapArchive): Flow<UnzipEvent> {
        return unarchiveDao.unarchive(archive.id)
    }
}