package com.peterlaurence.trekme.features.map.presentation.ui.trackcreate.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.peterlaurence.trekme.features.map.presentation.ui.components.makeOffset
import com.peterlaurence.trekme.features.map.presentation.viewmodel.trackcreate.layer.TrackSegmentState
import com.peterlaurence.trekme.util.dpToPx
import kotlinx.coroutines.flow.StateFlow
import ovh.plrapps.mapcompose.api.DefaultCanvas
import ovh.plrapps.mapcompose.api.scale
import ovh.plrapps.mapcompose.ui.state.MapState

@Composable
fun TrackLines(
    modifier: Modifier = Modifier,
    mapState: MapState,
    trackState: StateFlow<List<TrackSegmentState>>
) {
    val segments by trackState.collectAsState()
    if (segments.isEmpty()) return

    for (segment in segments) {
        key(segment.id) {
            DefaultCanvas(
                modifier = modifier,
                mapState = mapState
            ) {
                val p1 = makeOffset(segment.p1.x, segment.p1.y, mapState)
                val p2 = makeOffset(segment.p2.x, segment.p2.y, mapState)

                drawLine(
                    lineColor,
                    start = p1,
                    end = p2,
                    strokeWidth = lineWidthPx / mapState.scale,
                    cap = StrokeCap.Round
                )

                drawCircle(lineColor, center = p1, radius = mainNodeRadiusPx / mapState.scale)
                drawCircle(lineColor, center = (p1 + p2) / 2f, radius = secondaryNodeRadiusPx / mapState.scale)
                drawCircle(lineColor, center = p2, radius = mainNodeRadiusPx / mapState.scale)
            }
        }
    }
}

private val lineColor = Color(0xFF311B92)
private val lineWidthPx = dpToPx(4)
private val secondaryNodeRadiusPx = dpToPx(4)
private val mainNodeRadiusPx = dpToPx(8)