package com.peterlaurence.trekme.ui.mapview.components

import android.content.Context
import android.util.AttributeSet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.AbstractComposeView
import com.peterlaurence.trekme.features.map.presentation.ui.components.TopOverlay
import com.peterlaurence.trekme.ui.mapview.DistanceLayer
import com.peterlaurence.trekme.ui.mapview.MapViewFragment.SpeedListener
import com.peterlaurence.trekme.ui.theme.TrekMeTheme


class TopOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle), SpeedListener, DistanceLayer.DistanceListener {

    private var speed by mutableStateOf<Float?>(null)
    private var distance by mutableStateOf(0f)
    private var speedVisibility by mutableStateOf(false)
    private var distanceVisibility by mutableStateOf(false)

    @Composable
    override fun Content() {
        TrekMeTheme {
            TopOverlay(speed, distance, speedVisibility, distanceVisibility)
        }
    }

    override fun onSpeed(speed: Float) {
        this.speed = speed
    }

    override fun setSpeedVisible(v: Boolean) {
        speedVisibility = v
    }

    override fun toggleSpeedVisibility(): Boolean {
        speedVisibility = !speedVisibility
        return speedVisibility
    }

    override fun hideSpeed() {
        speedVisibility = false
    }

    override fun onDistance(distance: Float) {
        this.distance = distance
    }

    override fun toggleDistanceVisibility() {
        distanceVisibility = !distanceVisibility
    }

    override fun showDistance() {
        distanceVisibility = true
    }

    override fun hideDistance() {
        distanceVisibility = false
    }
}