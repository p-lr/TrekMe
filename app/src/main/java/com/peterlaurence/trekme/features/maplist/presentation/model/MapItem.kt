package com.peterlaurence.trekme.features.maplist.presentation.model

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.*

class MapItem(val mapId: UUID) {
    var isFavorite: Boolean by mutableStateOf(false)
    var title: String by mutableStateOf("")
    var image: Bitmap? by mutableStateOf(null)

    override fun equals(other: Any?): Boolean {
        if (other !is MapItem) return false
        return mapId == other.mapId
                && isFavorite == other.isFavorite
                && title == other.title
                && image == other.image
    }

    override fun hashCode(): Int {
        var result = mapId.hashCode()
        result = 31 * result + isFavorite.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (image?.hashCode() ?: 0)
        return result
    }
}