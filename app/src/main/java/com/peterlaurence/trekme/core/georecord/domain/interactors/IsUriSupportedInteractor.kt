package com.peterlaurence.trekme.core.georecord.domain.interactors

import android.content.ContentResolver
import android.net.Uri
import com.peterlaurence.trekme.core.georecord.domain.model.supportedGeoRecordFilesExtensions
import com.peterlaurence.trekme.util.FileUtils
import javax.inject.Inject

class IsUriSupportedInteractor @Inject constructor(){
    fun isUriSupported(uri: Uri, contentResolver: ContentResolver): Boolean {
        val fileName = FileUtils.getFileRealFileNameFromURI(contentResolver, uri)
        val extension = fileName?.substringAfterLast('.', "")

        if ("" == extension) return false

        return supportedGeoRecordFilesExtensions.any { it == extension }
    }
}