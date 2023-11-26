package com.peterlaurence.trekme.features.trailsearch.presentation.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import com.peterlaurence.trekme.core.units.UnitFormatter
import com.peterlaurence.trekme.features.common.presentation.ui.widgets.DialogShape

@Composable
fun Cursor(
    modifier: Modifier = Modifier,
    distance: Double,
    elevation: Double
) {
    val radius = with(LocalDensity.current) { 10.dp.toPx() }
    val nubWidth = with(LocalDensity.current) { 20.dp.toPx() }
    val nubHeight = with(LocalDensity.current) { 18.dp.toPx() }

    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        shape = DialogShape(
            radius,
            DialogShape.NubPosition.BOTTOM,
            0.5f,
            nubWidth = nubWidth,
            nubHeight = nubHeight,
        )
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(
                text = stringResource(id = R.string.distance_cursor) +
                        " ${UnitFormatter.formatDistance(distance)}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = stringResource(id = R.string.elevation_cursor) +
                        " ${UnitFormatter.formatElevation(elevation)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}