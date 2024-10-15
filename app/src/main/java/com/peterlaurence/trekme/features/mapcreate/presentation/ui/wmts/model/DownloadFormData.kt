package com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model

import android.os.Parcelable
import com.peterlaurence.trekme.core.wmts.domain.model.WmtsSource
import kotlinx.parcelize.Parcelize

@Parcelize
data class DownloadFormData(
    val wmtsSource: WmtsSource,
    val p1: Point,
    val p2: Point,
    val levelMin: Int = 1,
    val levelMax: Int = 18,
    val startMinLevel: Int = 12,
    val startMaxLevel: Int = 16,
    val tilesNumberLimit: Long? = null
) : Parcelable