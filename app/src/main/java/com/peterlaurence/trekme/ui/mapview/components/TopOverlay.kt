package com.peterlaurence.trekme.ui.mapview.components

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.ui.mapview.DistanceLayer
import com.peterlaurence.trekme.ui.mapview.MapViewFragment.SpeedListener
import com.peterlaurence.trekme.ui.theme.TrekMeTheme

/**
 * An overlay to show optional information. It can display :
 * • The current speed
 * • The distance between two points
 *
 * @author P.Laurence on 2017/06/03 -- converted to compose on 2021/11/06
 */
@Composable
fun TopOverlay(
    speed: Float?,
    distance: Float,
    speedVisibility: Boolean,
    distanceVisibility: Boolean
) {
    Row(Modifier.background(colorResource(id = R.color.colorIndicatorOverlay))) {
        if (speedVisibility) {
            Text(
                text = if (speed != null) {
                    UnitFormatter.formatSpeed(speed.toDouble())
                } else stringResource(
                    id = R.string.mapview_acq_gps
                ),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 18.sp,
                color = colorResource(id = R.color.colorPrimaryTextWhite)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (distanceVisibility) {
            Text(
                text = UnitFormatter.formatDistance(distance.toDouble()),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 18.sp,
                color = colorResource(id = R.color.colorPrimaryTextWhite)
            )
        }
    }
}

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