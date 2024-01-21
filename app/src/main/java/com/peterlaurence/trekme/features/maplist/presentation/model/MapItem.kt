package com.peterlaurence.trekme.features.maplist.presentation.model

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.StateFlow
import java.util.*

@Stable
data class MapItem(val mapId: UUID, val titleFlow: StateFlow<String>, val image: StateFlow<Bitmap?>, val isFavorite: Boolean = false)