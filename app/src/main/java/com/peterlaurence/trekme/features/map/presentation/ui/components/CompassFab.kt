package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.peterlaurence.trekme.R

@Composable
fun CompassFab(degrees: Float, onClick: () -> Unit) {
    FloatingActionButton(
        onClick,
        modifier = Modifier.rotate(degrees),
        containerColor = Color.White,
        shape = CircleShape
    ) {
        Image(
            painter = painterResource(id = R.drawable.compass),
            contentDescription = stringResource(id = R.string.compass_fab_desc),
        )
    }
}