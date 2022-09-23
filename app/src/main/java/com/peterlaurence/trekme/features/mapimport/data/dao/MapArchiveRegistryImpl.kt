package com.peterlaurence.trekme.features.mapimport.data.dao

import androidx.documentfile.provider.DocumentFile
import com.peterlaurence.trekme.features.mapimport.domain.dao.MapArchiveRegistry
import java.util.*

class MapArchiveRegistryImpl: MapArchiveRegistry {
    private val _docForId = mutableMapOf<UUID, DocumentFile>()

    override fun getDocumentForId(uuid: UUID): DocumentFile? {
        return _docForId[uuid]
    }

    override fun setDocumentForId(uuid: UUID, doc: DocumentFile) {
        _docForId[uuid] = doc
    }
}