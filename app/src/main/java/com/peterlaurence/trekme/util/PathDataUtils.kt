package com.peterlaurence.trekme.util

import androidx.compose.ui.graphics.vector.PathNode


/**
 * Linearly interpolates two lists of path nodes to simulate path morphing.
 */
fun lerp(fromPathNodes: List<PathNode>, toPathNodes: List<PathNode>, t: Float): List<PathNode> {
    return fromPathNodes.mapIndexed { i, from ->
        val to = toPathNodes[i]
        if (from is PathNode.MoveTo && to is PathNode.MoveTo) {
            PathNode.MoveTo(
                lerp(from.x, to.x, t),
                lerp(from.y, to.y, t),
            )
        } else if (from is PathNode.CurveTo && to is PathNode.CurveTo) {
            PathNode.CurveTo(
                lerp(from.x1, to.x1, t),
                lerp(from.y1, to.y1, t),
                lerp(from.x2, to.x2, t),
                lerp(from.y2, to.y2, t),
                lerp(from.x3, to.x3, t),
                lerp(from.y3, to.y3, t),
            )
        } else if (from is PathNode.RelativeCurveTo && to is PathNode.RelativeCurveTo) {
            PathNode.RelativeCurveTo(
                lerp(from.dx1, to.dx1, t),
                lerp(from.dy1, to.dy1, t),
                lerp(from.dx2, to.dx2, t),
                lerp(from.dy2, to.dy2, t),
                lerp(from.dx3, to.dx3, t),
                lerp(from.dy3, to.dy3, t),
            )
        } else if (from is PathNode.RelativeReflectiveCurveTo && to is PathNode.RelativeReflectiveCurveTo){
            PathNode.RelativeReflectiveCurveTo(
                lerp(from.dx1, to.dx1, t),
                lerp(from.dy1, to.dy1, t),
                lerp(from.dx2, to.dx2, t),
                lerp(from.dy2, to.dy2, t)
            )
        } else if (from is PathNode.LineTo && to is PathNode.LineTo) {
            PathNode.LineTo(
                lerp(from.x, to.x, t),
                lerp(from.y, to.y, t)
            )
        } else if (from is PathNode.Close && to is PathNode.Close) {
            PathNode.Close
        } else {
            // TODO: support all possible SVG path data types
            throw IllegalStateException("Unsupported SVG PathNode command")
        }
    }
}