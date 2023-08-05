package com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class PrimaryLayerSelectionData(
    val layerIdsAndAvailability: List<Pair<String, Boolean>>, val selectedLayerId: String
) : Parcelable