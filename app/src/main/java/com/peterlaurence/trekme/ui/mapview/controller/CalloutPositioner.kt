package com.peterlaurence.trekme.ui.mapview.controller

import com.peterlaurence.mapview.MapView

data class CalloutPosition(val relativeAnchorTop: Float, val absoluteAnchorTop: Float,
                           val relativeAnchorLeft: Float, val absoluteAnchorLeft: Float)

/**
 * Positions the callout based on provided info.
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
    val yInPx: Int = mapView.coordinateTranslater.translateAndScaleY(relativeY, mapView.scale)
    val xInPx: Int = mapView.coordinateTranslater.translateAndScaleX(relativeX, mapView.scale)

    val limitBottom: Float = mapView.coordinateTranslater.baseHeight * mapView.scale
    val roomFullTop = calloutHeight + markerHeight / 2f < yInPx
    val roomCenteredTop = calloutHeight / 2f < yInPx
    val roomFullBottom = calloutHeight + yInPx < limitBottom
    val roomCenteredBottom = yInPx + calloutHeight / 2f < limitBottom

    val limitRight: Float = mapView.coordinateTranslater.baseWidth * mapView.scale
    val roomCenteredLeft = calloutWidth / 2f < xInPx
    val roomFullLeft = calloutWidth + markerWidth / 2f < xInPx
    val roomCenteredRight = xInPx + calloutWidth / 2f < limitRight
    val roomFullRight = xInPx + calloutWidth + markerWidth / 2f < limitRight

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