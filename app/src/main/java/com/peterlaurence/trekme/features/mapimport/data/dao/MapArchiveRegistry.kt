package com.peterlaurence.trekme.features.mapimport.data.dao

import androidx.documentfile.provider.DocumentFile
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapArchiveRegistry @Inject constructor() {
    private val _docForId = mutableMapOf<UUID, DocumentFile>()

    fun getDocumentForId(uuid: UUID): DocumentFile? {
        return _docForId[uuid]
    }

    fun setDocumentForId(uuid: UUID, doc: DocumentFile) {
        _docForId[uuid] = doc
    }
}