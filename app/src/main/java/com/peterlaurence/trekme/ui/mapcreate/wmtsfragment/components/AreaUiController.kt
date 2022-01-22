package com.peterlaurence.trekme.ui.mapcreate.wmtsfragment.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.peterlaurence.trekme.R
import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState


/**
 * Controls drag gestures on markers which define the selection area, using MapCompose api.
 *
 * @author P.Laurence on 2021/08/19
 */
class AreaUiController {
    var p1x by mutableStateOf(0.0)
    var p1y by mutableStateOf(0.0)
    var p2x by mutableStateOf(0.0)
    var p2y by mutableStateOf(0.0)
    private var pcx by mutableStateOf(0.0)
    private var pcy by mutableStateOf(0.0)

    /**
     * Attach and initialize points positions.
     */
    suspend fun attachAndInit(state: MapState) {
        val box = state.visibleBoundingBox()

        /* Initial positions */
        p1x = (box.xLeft + (box.xRight - box.xLeft) * 0.2).coerceIn(0.0, 1.0)
        p1y = (box.yTop + (box.yBottom - box.yTop) * 0.2).coerceIn(0.0, 1.0)
        p2x = (box.xLeft + (box.xRight - box.xLeft) * 0.8).coerceIn(0.0, 1.0)
        p2y = (box.yTop + (box.yBottom - box.yTop) * 0.8).coerceIn(0.0, 1.0)
        pcx = (p1x + p2x) / 2
        pcy = (p1y + p2y) / 2

        attach(state)
    }

    /**
     * Attach but keep previous configuration.
     */
    fun attach(state: MapState) {
        state.addMarker(m1, p1x, p1y, Offset(-0.5f, -0.5f)) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(colorResource(id = R.color.colorAreaMarker))
                    .clip(CircleShape)
            )
        }
        state.addMarker(m2, p2x, p2y, Offset(-0.5f, -0.5f)) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(colorResource(id = R.color.colorAreaMarker))
                    .clip(CircleShape)
            )
        }
        state.addMarker(central, pcx, pcy, Offset(-0.5f, -0.5f)) {
            Image(
                painter = painterResource(id = R.drawable.area_move_marker),
                contentDescription = null
            )
        }

        state.enableMarkerDrag(m1) { id, x, y, dx, dy, _, _ ->
            p1x = x + dx
            p1y = y + dy
            updateCentralMarker(state)
            state.moveMarker(id, p1x, p1y)
        }
        state.enableMarkerDrag(m2) { id, x, y, dx, dy, _, _ ->
            p2x = x + dx
            p2y = y + dy
            updateCentralMarker(state)
            state.moveMarker(id, p2x, p2y)
        }
        state.enableMarkerDrag(central) { _, _, _, dx, dy, _, _ ->
            p1x = (p1x + dx).coerceIn(0.0, 1.0)
            p1y = (p1y + dy).coerceIn(0.0, 1.0)
            p2x = (p2x + dx).coerceIn(0.0, 1.0)
            p2y = (p2y + dy).coerceIn(0.0, 1.0)
            state.moveMarker(m1, p1x, p1y)
            state.moveMarker(m2, p2x, p2y)
            updateCentralMarker(state)
        }
    }

    fun detach(state: MapState) {
        state.removeMarker(m1)
        state.removeMarker(m2)
        state.removeMarker(central)
    }

    private fun updateCentralMarker(state: MapState) {
        pcx = (p1x + p2x) / 2
        pcy = (p1y + p2y) / 2
        state.moveMarker(central, pcx, pcy)
    }

    private val m1 = "m1"
    private val m2 = "m2"
    private val central = "central"
}