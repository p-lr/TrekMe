package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.theme.md_theme_dark_surface

@Composable
fun ZoomIndicator(modifier: Modifier = Modifier, zoom: Float) {
    Row(
        modifier
            .background(
                md_theme_dark_surface.copy(alpha = 0.5f),
                RoundedCornerShape(50)

            )
            .padding(start = 4.dp, end = 6.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            painter = painterResource(R.drawable.ic_baseline_search_24),
            contentDescription = null,
            tint = Color.White,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = "%.1f".format(zoom),
            color = Color.White,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Preview(showBackground = true, widthDp = 150, heightDp = 70)
@Composable
private fun ZoomIndicatorPreview() {
    TrekMeTheme {
        Column(Modifier.padding(16.dp)) {
            ZoomIndicator(zoom = 13.6124f)
        }
    }
}