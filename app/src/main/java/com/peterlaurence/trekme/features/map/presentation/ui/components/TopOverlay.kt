package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.units.UnitFormatter

/**
 * An overlay at the top of the screen, just below the topbar, to show optional information.
 * It can display :
 * • The current speed
 * • The distance between two points
 *
 * @since 2017/06/03 -- converted to compose on 2021/11/06
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