package com.peterlaurence.trekme.features.common.presentation.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingScreen() {
    Box(Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            Modifier
                .align(Alignment.Center)
                .width(100.dp)
        )
    }
}