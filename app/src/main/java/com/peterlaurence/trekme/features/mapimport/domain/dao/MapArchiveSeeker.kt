package com.peterlaurence.trekme.features.mapimport.domain.dao

import android.net.Uri
import androidx.documentfile.provider.DocumentFile

interface MapArchiveSeeker {
    suspend fun seek(uri: Uri): List<DocumentFile>
}