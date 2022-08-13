package com.peterlaurence.trekme.features.shop.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun Header(title: String, subTitle: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF448AFF),
                            Color(0xFF40C4FF)
                        ),
                        center = Offset.Zero,
                        radius = size.width
                    ),
                    size = size
                )
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            color = Color.White,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp
        )
        if (subTitle != null) {
            Text(
                subTitle,
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}