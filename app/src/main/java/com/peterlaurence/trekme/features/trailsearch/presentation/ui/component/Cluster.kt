package com.peterlaurence.trekme.features.trailsearch.presentation.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Cluster(size: Int) {
    /* Here we can customize the cluster style */
    Box(
        modifier = Modifier
            .background(
                Color(0xcc1565c0),
                shape = CircleShape
            )
            .size(50.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = size.toString(), color = Color.White)
    }
}