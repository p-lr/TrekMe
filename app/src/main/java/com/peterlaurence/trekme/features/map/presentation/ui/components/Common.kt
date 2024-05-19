package com.peterlaurence.trekme.features.map.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.units.UnitFormatter
import ovh.plrapps.mapcompose.api.fullSize
import ovh.plrapps.mapcompose.ui.state.MapState
import java.math.RoundingMode
import java.text.DecimalFormat

fun makeOffset(x: Double, y: Double, mapState: MapState): Offset {
    return Offset(
        x = (mapState.fullSize.width * x).toFloat(),
        y = (mapState.fullSize.height * y).toFloat()
    )
}

/**
 * If we have the location, we display this information right below the latitude and longitude.
 * The latitude and longitude are displayed in the callout with a 4-digit precision, which is
 * acceptable in this context. The 6-digit precision is accessible in the marker properties.
 */
@Composable
fun makeMarkerSubtitle(
    latitude: Double,
    longitude: Double,
    distanceInMeters: Double?
): AnnotatedString {
    val latShort = stringResource(id = R.string.latitude_short)
    val lngShort = stringResource(id = R.string.longitude_short)
    val dstShort = stringResource(id = R.string.distance_short)
    return buildAnnotatedString {
        withStyle(ParagraphStyle(lineHeight = 12.sp)) {
            append("$latShort : ${df.format(latitude)}  " + "$lngShort : ${df.format(longitude)}")
            if (distanceInMeters != null) {
                appendLine()
                append("$dstShort : ${UnitFormatter.formatDistance(distanceInMeters)}")
            }
        }
    }
}

private val df = DecimalFormat("#.####").apply {
    roundingMode = RoundingMode.CEILING
}