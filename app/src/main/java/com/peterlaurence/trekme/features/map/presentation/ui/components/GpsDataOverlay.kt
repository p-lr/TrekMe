package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.location.domain.model.InternalGps
import com.peterlaurence.trekme.core.location.domain.model.Location
import com.peterlaurence.trekme.core.location.domain.model.LocationProducerBtInfo
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.common.presentation.ui.theme.TrekMeTheme
import com.peterlaurence.trekme.features.common.presentation.ui.theme.md_theme_dark_surface
import kotlin.time.TimeSource


@Composable
fun GpsDataOverlay(
    modifier: Modifier = Modifier,
    location: Location?,
    isElevationModifiable: Boolean,
    elevationFix: Int,
    lastUpdateInSeconds: Long?,
    onFixElevationClick: () -> Unit = {}
) {
    Column(
        modifier
            .width(IntrinsicSize.Min)
            .background(
                md_theme_dark_surface.copy(alpha = 0.5f),
                RoundedCornerShape(topEnd = 16.dp)
            )
            .padding(8.dp)
    ) {
        /* When an external source is used, display the name of the device */
        when (val p = location?.locationProducerInfo) {
            is LocationProducerBtInfo -> Text(
                p.name,
                Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = colorResource(id = R.color.colorPrimaryTextWhite),
                fontFamily = FontFamily.Monospace
            )

            /* Nothing to do */
            InternalGps, null -> {}
        }
        KeyValueRow(key = stringResource(id = R.string.latitude_short),
            value = location?.latitude?.let { UnitFormatter.formatLatLon(location.latitude) }
                ?: "")
        KeyValueRow(key = stringResource(id = R.string.longitude_short),
            value = location?.longitude?.let { UnitFormatter.formatLatLon(location.longitude) }
                ?: "")
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (isElevationModifiable) {
                        Modifier.clickable(onClick = onFixElevationClick)
                    } else Modifier
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            KeyValueRow(key = stringResource(id = R.string.elevation_short),
                value = location?.altitude?.let { UnitFormatter.formatElevation(location.altitude + elevationFix) }
                    ?: "")
            if (isElevationModifiable) {
                Image(
                    painter = painterResource(id = R.drawable.ic_edit_black_24dp),
                    modifier = Modifier
                        .background(Color(0x45FFFFFF), shape = CircleShape)
                        .clip(CircleShape)
                        .padding(3.dp)
                        .alpha(1f)
                        .size(17.dp),
                    colorFilter = ColorFilter.tint(Color.White),
                    contentDescription = null
                )
            }
        }

        if (lastUpdateInSeconds != null) {
            KeyValueRow(
                key = stringResource(id = R.string.update_short),
                value = UnitFormatter.formatDuration(lastUpdateInSeconds)
            )
        }
    }
}

@Composable
private fun KeyValueRow(key: String, value: String) {
    Row {
        Text(key, color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.width(8.dp))
        Text(value, color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

@Preview
@Composable
private fun GpsDataOverlayPreview() {
    TrekMeTheme {
        GpsDataOverlay(
            location = Location(
                2.67,
                54.78,
                altitude = 100.2,
                markedTime = TimeSource.Monotonic.markNow(),
                locationProducerInfo = LocationProducerBtInfo("Garmin", "")
            ),
            isElevationModifiable = true,
            lastUpdateInSeconds = 4,
            elevationFix = 0
        )
    }
}



