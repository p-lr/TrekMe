package com.peterlaurence.trekme.features.mapimport.domain.interactor

import android.net.Uri
import com.peterlaurence.trekme.features.mapimport.domain.dao.MapArchiveRegistry
import com.peterlaurence.trekme.features.mapimport.domain.dao.MapArchiveSeeker
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
    private val mapArchiveSeeker: MapArchiveSeeker,
    private val unarchiveDao: UnarchiveDao,
) {
    suspend fun seekArchivesAtLocation(uri: Uri) {
        val docs = mapArchiveSeeker.seek(uri)

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