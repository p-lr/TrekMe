package com.peterlaurence.trekme.features.maplist.presentation.model

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import java.util.*

@Stable
data class MapItem(val mapId: UUID, val title: String, val isFavorite: Boolean = false, val image: Bitmap? = null)