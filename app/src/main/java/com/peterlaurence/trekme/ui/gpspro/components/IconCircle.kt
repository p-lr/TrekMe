package com.peterlaurence.trekme.ui.gpspro.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun IconCircle(backgroundColor: Color, @DrawableRes iconId: Int) {
    Surface(
            modifier = Modifier.size(40.dp)
                    .clip(CircleShape),
            color = backgroundColor.copy(alpha = 0.2f)

    ) {
        Icon(
                painter = painterResource(id = iconId),
                tint = backgroundColor,
                contentDescription = null,
                modifier = Modifier.padding(6.dp)
        )
    }
}
