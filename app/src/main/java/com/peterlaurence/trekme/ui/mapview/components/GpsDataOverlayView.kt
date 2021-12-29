package com.peterlaurence.trekme.ui.mapview.components

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.peterlaurence.trekme.core.location.Location
import com.peterlaurence.trekme.features.map.presentation.ui.components.GpsDataOverlay
import com.peterlaurence.trekme.ui.theme.TrekMeTheme

class GpsDataOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {
    var location by mutableStateOf<Location?>(null)

    @Composable
    override fun Content() {
        TrekMeTheme {
            GpsDataOverlay(location)
        }
    }

    fun setData(location: Location) {
        this.location = location
    }
}