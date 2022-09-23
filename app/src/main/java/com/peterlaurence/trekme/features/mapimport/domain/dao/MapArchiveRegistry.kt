package com.peterlaurence.trekme.features.mapimport.domain.dao

import androidx.documentfile.provider.DocumentFile
import java.util.UUID

interface MapArchiveRegistry {
    fun setDocumentForId(uuid: UUID, doc: DocumentFile)
    fun getDocumentForId(uuid: UUID): DocumentFile?
}