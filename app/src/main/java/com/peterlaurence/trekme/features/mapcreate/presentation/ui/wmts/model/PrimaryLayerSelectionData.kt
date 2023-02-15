package com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class PrimaryLayerSelectionData(
    val layerIds: List<String>, val selectedLayerId: String
) : Parcelable