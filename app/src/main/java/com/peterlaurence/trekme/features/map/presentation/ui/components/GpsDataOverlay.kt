package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.location.Location
import com.peterlaurence.trekme.core.location.LocationProducerBtInfo
import com.peterlaurence.trekme.core.units.UnitFormatter


@Composable
fun GpsDataOverlay(location: Location?, modifier: Modifier = Modifier) {
    Column(
        modifier
            .width(IntrinsicSize.Min)
            .background(
                colorResource(id = R.color.colorIndicatorOverlay),
                RoundedCornerShape(topEnd = 5.dp)
            )
            .padding(8.dp)
    ) {
        /* When an external source is used, display the name of the device */
        when (val p = location?.locationProducerInfo) {
            is LocationProducerBtInfo -> Text(
                p.name,
                Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = colorResource(id = R.color.colorPrimaryTextWhite)
            )
        }
        KeyValueRow(key = stringResource(id = R.string.latitude_short),
            value = location?.latitude?.let { UnitFormatter.formatLatLon(location.latitude) }
                ?: "")
        KeyValueRow(key = stringResource(id = R.string.longitude_short),
            value = location?.longitude?.let { UnitFormatter.formatLatLon(location.longitude) }
                ?: "")
        KeyValueRow(key = stringResource(id = R.string.elevation_short),
            value = location?.altitude?.let { UnitFormatter.formatElevation(location.altitude) }
                ?: "")
    }
}

@Composable
private fun KeyValueRow(key: String, value: String) {
    val color = colorResource(id = R.color.colorPrimaryTextWhite)
    Row {
        Text(key, color = color, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.width(8.dp))
        Text(value, color = color, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}



