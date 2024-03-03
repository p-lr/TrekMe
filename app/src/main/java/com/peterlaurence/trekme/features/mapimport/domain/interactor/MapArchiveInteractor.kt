package com.peterlaurence.trekme.features.mapimport.domain.interactor

import android.net.Uri
import com.peterlaurence.trekme.features.mapimport.domain.dao.MapArchiveSeeker
import com.peterlaurence.trekme.features.mapimport.domain.dao.UnarchiveDao
import com.peterlaurence.trekme.features.mapimport.domain.model.MapArchive
import com.peterlaurence.trekme.features.mapimport.domain.model.UnzipEvent
import com.peterlaurence.trekme.features.mapimport.domain.repository.MapArchiveRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MapArchiveInteractor @Inject constructor(
    private val repository: MapArchiveRepository,
    private val mapArchiveSeeker: MapArchiveSeeker,
    private val unarchiveDao: UnarchiveDao,
) {
    suspend fun seekArchivesAtLocation(uri: Uri) {
        val archives = mapArchiveSeeker.seek(uri)
        repository.setArchives(archives)
    }

    /**
     * Launch the decompression of an archive.
     */
    suspend fun unarchiveAndGetEvents(archive: MapArchive): Flow<UnzipEvent> {
        return unarchiveDao.unarchive(archive.id)
    }
}