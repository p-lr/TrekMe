package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.runtime.*
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
    backgroundColor = color,
    strokeColor = strokeColor,
    isStatic = isStatic
)

private val color = Color(0xFF9C27B0)
private val strokeColor = Color(0xFF4A148C)