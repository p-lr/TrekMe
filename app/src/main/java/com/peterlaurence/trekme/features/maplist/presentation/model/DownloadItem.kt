package com.peterlaurence.trekme.features.maplist.presentation.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class DownloadItem {
    var progress: Int by mutableStateOf(0)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DownloadItem

        if (progress != other.progress) return false

        return true
    }

    override fun hashCode(): Int {
        return progress
    }
}
