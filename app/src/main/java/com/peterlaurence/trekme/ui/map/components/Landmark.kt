package com.peterlaurence.trekme.ui.map.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * A landmark is just a [Marker] with different colors.
 */
@Composable
fun LandMark(
    modifier: Modifier = Modifier,
    isStatic: Boolean
) = Marker(
    modifier,
    backgroundColor = Color(0xFF9C27B0),
    strokeColor = Color(0xFF4A148C),
    isStatic = isStatic
)