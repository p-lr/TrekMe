package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.ui.geometry.Offset
import ovh.plrapps.mapcompose.api.fullSize
import ovh.plrapps.mapcompose.ui.state.MapState

fun makeOffset(x: Double, y: Double, mapState: MapState): Offset {
    return Offset(
        x = (mapState.fullSize.width * x).toFloat(),
        y = (mapState.fullSize.height * y).toFloat()
    )
}