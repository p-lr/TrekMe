package com.peterlaurence.trekme.features.common.presentation.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        if (message != null) {
            Text(text = message)
            Spacer(modifier = Modifier.height(16.dp))
        }
        LinearProgressIndicator(
            Modifier.width(200.dp),
            strokeCap = StrokeCap.Round
        )
        Spacer(modifier = Modifier.weight(4f))
    }
}