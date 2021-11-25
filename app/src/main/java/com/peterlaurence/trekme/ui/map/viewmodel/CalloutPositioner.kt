package com.peterlaurence.trekme.ui.map.viewmodel

import ovh.plrapps.mapcompose.api.*
import ovh.plrapps.mapcompose.ui.state.MapState
import ovh.plrapps.mapcompose.utils.Point

data class CalloutPosition(val relativeAnchorTop: Float, val absoluteAnchorTop: Float,
                           val relativeAnchorLeft: Float, val absoluteAnchorLeft: Float)

/**
 * Positions the callout, taking into account the available space around the marker. Accounts for
 * the current visible bounding box of [MapState] (the visible part).
 * The marker is assumed to be centered on the provided position.
 *
 * The algorithm tries the following positions relative to the marker (x):
 *
 *                    5 1 6
 *                    4 x 3
 *                    8 2 7
 *
 * @param calloutWidth The width of the callout view in pixels
 * @param calloutHeight The height of the callout view in pixels
 * @param relativeX The normalized X position of the marker
 * @param relativeY The normalized Y position of the marker
 * @param markerWidth The width of the marker in pixels
 * @param markerHeight The height of the marker in pixels
 */
suspend fun positionCallout(
    mapState: MapState, calloutWidth: Int, calloutHeight: Int,
    relativeX: Double, relativeY: Double,
    markerWidth: Int, markerHeight: Int
): CalloutPosition {

    /* We're working in the referential of the visible area, so we need to rotate the callout position */
    val rotatedPoint = mapState.rotatePoint(Point(relativeX, relativeY), mapState.rotation)

    val xInPx = rotatedPoint.x * mapState.fullSize.width * mapState.scale
    val yInPx = rotatedPoint.y * mapState.fullSize.height * mapState.scale

    val boundingBox = mapState.visibleBoundingBox().scale(
        mapState.fullSize.width * mapState.scale.toDouble(),
        mapState.fullSize.height * mapState.scale.toDouble()
    )

    val limitBottom = mapState.fullSize.height * mapState.scale.toDouble()
    val spaceBottom = boundingBox.yBottom.coerceAtMost(limitBottom) - yInPx
    val spaceTop = yInPx - boundingBox.yTop.coerceAtLeast(0.0)
    val roomFullTop = calloutHeight + markerHeight / 2f < spaceTop
    val roomCenteredTop = calloutHeight / 2f < spaceTop
    val roomFullBottom = calloutHeight < spaceBottom
    val roomCenteredBottom = calloutHeight / 2f < spaceBottom

    val limitRight = mapState.fullSize.width * mapState.scale.toDouble()
    val spaceRight = boundingBox.xRight.coerceAtMost(limitRight) - xInPx
    val spaceLeft = xInPx - boundingBox.xLeft.coerceAtLeast(0.0)
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

private fun BoundingBox.scale(xFactor: Double, yFactor: Double): BoundingBox {
    return BoundingBox(xLeft * xFactor, yTop * yFactor, xRight * xFactor, yBottom * yFactor)
}