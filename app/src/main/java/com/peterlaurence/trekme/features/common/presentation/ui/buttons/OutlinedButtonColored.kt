package com.peterlaurence.trekme.features.common.presentation.ui.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R

@Composable
fun OutlinedButtonColored(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = colorResource(id = R.color.colorAccent),
    text: String,
    shape: Shape = MaterialTheme.shapes.small
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        border= BorderStroke(1.dp, color),
        shape = shape
    ) {
        Text(text, color = color)
    }
}