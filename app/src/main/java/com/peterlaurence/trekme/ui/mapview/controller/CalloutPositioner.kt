package com.peterlaurence.trekme.ui.mapview.controller

import ovh.plrapps.mapview.MapView
import ovh.plrapps.mapview.api.getVisibleViewport

data class CalloutPosition(val relativeAnchorTop: Float, val absoluteAnchorTop: Float,
                           val relativeAnchorLeft: Float, val absoluteAnchorLeft: Float)

/**
 * Positions the callout, taking into account the available space around the marker. Accounts for
 * the current scroll of the [MapView] (the visible part) and the limits of the [MapView].
 *
 * The algorithm tries the following positions relative to the marker (x):
 *
 *                    5 1 6
 *                    4 x 3
 *                    8 2 7
 *
 * @param calloutWidth The width of the callout view in pixels
 * @param calloutHeight The height of the callout view in pixels
 * @param relativeX The relative X position of the marker
 * @param relativeY The relative Y position of the marker
 * @param markerWidth The width of the marker in pixels
 * @param markerHeight The height of the marker in pixels
 */
fun positionCallout(mapView: MapView, calloutWidth: Int, calloutHeight: Int,
                    relativeX: Double, relativeY: Double,
                    markerWidth: Int, markerHeight: Int): CalloutPosition {

    val ct = mapView.coordinateTranslater ?: return CalloutPosition(-0.5f, 0f, -0.5f, 0f)
    val yInPx: Int = ct.translateAndScaleY(relativeY, mapView.scale)
    val xInPx: Int = ct.translateAndScaleX(relativeX, mapView.scale)

    val viewport = mapView.getVisibleViewport()

    val limitBottom = (ct.baseHeight * mapView.scale).toInt()
    val spaceBottom = viewport.bottom.coerceAtMost(limitBottom) - yInPx
    val spaceTop = yInPx - viewport.top.coerceAtLeast(0)
    val roomFullTop = calloutHeight + markerHeight / 2f < spaceTop
    val roomCenteredTop = calloutHeight / 2f < spaceTop
    val roomFullBottom = calloutHeight < spaceBottom
    val roomCenteredBottom = calloutHeight / 2f < spaceBottom

    val limitRight = (ct.baseWidth * mapView.scale).toInt()
    val spaceRight = viewport.right.coerceAtMost(limitRight) - xInPx
    val spaceLeft = xInPx - viewport.left.coerceAtLeast(0)
    val roomCenteredLeft = calloutWidth / 2f < spaceLeft
    val roomFullLeft = calloutWidth + markerWidth / 2f < spaceLeft
    val roomCenteredRight = calloutWidth / 2f < spaceRight
    val roomFullRight = calloutWidth + markerWidth / 2f < spaceRight

    // Top-centered
    if (roomFullTop && roomCenteredLeft && roomCenteredRight) {
        return CalloutPosition(-1f, -markerHeight / 2f, -0.5f, 0f)
    }

    // Bottom-centered
    if (roomFullBottom && roomCenteredLeft && roomCenteredRight) {
        return CalloutPosition(0f, 0f, -0.5f, 0f)
    }

    // Right-centered
    if (roomFullRight && roomCenteredBottom && roomCenteredTop) {
        return CalloutPosition(-0.5f, 0f, 0f, markerWidth / 2f)
    }

    // Left-centered
    if (roomFullLeft && roomCenteredBottom && roomCenteredTop) {
        return CalloutPosition(-0.5f, 0f, -1f, -markerWidth / 2f)
    }

    // Upper-left
    if (roomFullTop && roomFullLeft) {
        return CalloutPosition(-1f, -markerHeight / 2f, -1f, -markerWidth / 2f)
    }

    // Upper-right
    if (roomFullTop && roomFullRight) {
        return CalloutPosition(-1f, -markerHeight / 2f, 0f, markerWidth / 2f)
    }

    // Bottom-right
    if (roomFullBottom && roomFullRight) {
        return CalloutPosition(0f, 0f, 0f, markerWidth / 2f)
    }

    // Bottom-left
    if (roomFullBottom && roomFullLeft) {
        return CalloutPosition(0f, 0f, -1f, -markerWidth / 2f)
    }

    // Nothing matches, position the callout centered above the marker
    return CalloutPosition(-0.5f, 0f, -0.5f, 0f)
}