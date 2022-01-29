package com.peterlaurence.trekme.features.mapcreate.presentation.ui.wmts.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import com.peterlaurence.trekme.R

@Composable
fun PlaceMarker() {
    Image(
        painter = painterResource(id = R.drawable.ic_baseline_location_on_48),
        colorFilter = ColorFilter.tint(colorResource(id = R.color.colorMarkerStroke)),
        alpha = 0.85f,
        contentDescription = null)
}