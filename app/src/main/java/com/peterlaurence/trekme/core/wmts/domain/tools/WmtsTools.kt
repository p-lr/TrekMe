package com.peterlaurence.trekme.core.wmts.domain.tools

import com.peterlaurence.trekme.core.map.domain.models.CalibrationPoint
import com.peterlaurence.trekme.core.wmts.domain.model.*
import kotlin.math.*


/**
 * At level 0, a WMTS map (which use WebMercator projection) is contained in a single tile of
 * 256x256px.
 * Each level has twice more tiles than the precedent.
 * Boundaries of the map are WebMercator values of the top left and bottom right corners :
 *
 *    X0 = -20037508.3427892476
 *    Y0 = -X0
 *    --------------------------
 *    |                        |
 *    |                        |
 *    |                        |
 *    |        WMTS map        |
 *    |                        |
 *    |                        |
 *    |                        |
 *    |                        |
 *    --------------------------
 *                             X1 = -X0
 *                             Y1 = X0
 * This function builds the map as a [Sequence] of [Tile] which contains the area defined by the two
 * supplied points. This is done in two steps:
 *
 * * Start by finding the tiles at min and max levels that overlap the area. We refer to them as
 * minArea and maxArea respectively.
 * * Find the tiles at higher levels that cover the top left corner of the minArea but stop (at the
 * bottom right corner) to overlap just enough of the maxArea.
 *
 * This algorithm has the advantage of producing a map which is compatible with MapCompose, while
 * reducing the amount of unwanted tiles (in most cases), since it removes unwanted tiles at the
 * bottom right corner. However, it's not perfect.
 * If the min level is too small, the tile size in meters at this level is high and
 * potentially a large unwanted area will be covered at the top left corner of this level and hence
 * all higher levels. This can lead to downloading a lot of unwanted tiles.
 *
 * A rule of thumb is to set the min level to 12 or higher, and max level no more than 17.
 *
 * Calibration points are set on top-left and bottom-right corners. They are computed with the tiles
 * of the max level.
 *
 * @param point1 A [Point] at any corner
 * @param point2 A [Point] at the opposite corner of [point1]
 */
fun getMapSpec(levelMin: Int, levelMax: Int, point1: Point, point2: Point, tileSize: Int = TILE_SIZE_PX): MapSpec {
    val (XLeft, YTop, XRight, YBottom) = orderCoordinates(point1, point2)

    val levelBuilder = LevelBuilder(levelMin, levelMax, XLeft, YTop, XRight, YBottom)
    val tileSequence = getTileSequence(levelMin, levelMax, levelBuilder)
    val calibrationPoints = getCalibrationPoints(levelMax, levelBuilder)
    val mapSize = getMapSize(levelBuilder, levelMax, tileSize)

    return MapSpec(
        levelMin,
        levelMax,
        mapSize.widthPx,
        mapSize.heightPx,
        tileSequence,
        calibrationPoints,
        tileSize
    )
}

private fun getTileSequence(
    levelMin: Int,
    levelMax: Int,
    levelBuilder: LevelBuilder
): Sequence<Tile> {

    return Sequence {
        iterator {
            for (level in levelMin..levelMax) {
                with(levelBuilder.getLevelAreaForLevel(level) ?: return@iterator) {
                    for ((indexRow, i) in (rowTop..rowBottom).withIndex()) {
                        for ((indexCol, j) in (colLeft..colRight).withIndex()) {
                            yield(Tile(level, i, j, level - levelMin, indexRow, indexCol))
                        }
                    }
                }
            }
        }
    }
}

fun getNumberOfTiles(levelMin: Int, levelMax: Int, point1: Point, point2: Point): Long {
    val (XLeft, YTop, XRight, YBottom) = orderCoordinates(point1, point2)

    return getNumberOfTiles(levelMin, levelMax, XLeft, YTop, XRight, YBottom)
}

/**
 * Given a number of tiles, get the resulting approximate size in Mo.
 * [TILE_SIZE_IN_MO] is an average size.
 */
fun Long.toSizeInMo(): Long {
    return (this * TILE_SIZE_IN_MO).toLong()
}


private data class TopLeftToBottomRight(
    val XLeft: Double,
    val YTop: Double,
    val XRight: Double,
    val YBottom: Double
)

@Suppress("LocalVariableName")
private fun orderCoordinates(point1: Point, point2: Point): TopLeftToBottomRight {
    val XLeft = min(point1.X, point2.X)
    val YTop = max(point1.Y, point2.Y)
    val XRight = max(point1.X, point2.X)
    val YBottom = min(point1.Y, point2.Y)

    return TopLeftToBottomRight(XLeft, YTop, XRight, YBottom)
}

/**
 * Encapsulates the logic of which tiles should be taken, given:
 * * a min level
 * * a max level
 * * the projected coordinates of the corners of the desired visible area
 * Each level is represented by a [LevelArea].
 */
private class LevelBuilder(
    levelMin: Int,
    levelMax: Int,
    XLeft: Double,
    YTop: Double,
    XRight: Double,
    YBottom: Double
) {
    private val levelAreaMap = mutableMapOf<Int, LevelArea>()

    init {
        val areaLvlMin = getLevelArea(levelMin, XLeft, YTop, XRight, YBottom)
        levelAreaMap[levelMin] = areaLvlMin

        val areaLvlMax = getLevelArea(levelMax, XLeft, YTop, XRight, YBottom)

        var colLeft = areaLvlMin.colLeft
        var rowTop = areaLvlMin.rowTop
        for (level in (levelMin + 1)..levelMax) {
            colLeft *= 2
            rowTop *= 2
            levelAreaMap[level] = LevelArea(
                colLeft,
                rowTop,
                colRight = (areaLvlMax.colRight / 2.0.pow(levelMax - level)).toInt(),
                rowBottom = (areaLvlMax.rowBottom / 2.0.pow(levelMax - level)).toInt()
            )
        }
    }

    fun getLevelAreaForLevel(level: Int): LevelArea? {
        return levelAreaMap[level]
    }
}


private fun getNumberOfTiles(
    levelMin: Int,
    levelMax: Int,
    XLeft: Double,
    YTop: Double,
    XRight: Double,
    YBottom: Double
): Long {
    val levelBuilder = LevelBuilder(levelMin, levelMax, XLeft, YTop, XRight, YBottom)

    var count = 0L
    for (level in levelMin..levelMax) {
        with(levelBuilder.getLevelAreaForLevel(level) ?: return 0) {
            count += (rowBottom - rowTop + 1).toLong() * (colRight - colLeft + 1).toLong()
        }
    }
    return count
}

/**
 * The size of a map is by convention the size in pixels of the maximum level.
 * Beware that we still need the minimum level as input, because of the way [LevelBuilder] works.
 */
private fun getMapSize(
    levelBuilder: LevelBuilder,
    levelMax: Int,
    tileSize: Int
): MapSize {
    val levelMaxArea = levelBuilder.getLevelAreaForLevel(levelMax) ?: return MapSize(0, 0)
    return with(levelMaxArea) {
        MapSize((colRight - colLeft + 1) * tileSize, (rowBottom - rowTop + 1) * tileSize)
    }
}

private data class MapSize(val widthPx: Int, val heightPx: Int)

private fun getCalibrationPoints(
    level: Int,
    levelBuilder: LevelBuilder,
): Pair<CalibrationPoint, CalibrationPoint> {
    val (colLeft, rowTop, colRight, rowBottom) = levelBuilder.getLevelAreaForLevel(level) ?:
        return Pair(
            CalibrationPoint(0.0, 0.0, 0.0, 0.0),
            CalibrationPoint(1.0, 1.0, 0.0, 0.0)
        )
    val tileSize = getTileInMetersForZoom(level)

    val topLeftCalibrationPoint = CalibrationPoint(
        0.0, 0.0, colLeft * tileSize + X0, Y0 - rowTop * tileSize
    )

    val bottomRightCalibrationPoint = CalibrationPoint(
        1.0, 1.0, (colRight + 1) * tileSize + X0, Y0 - (rowBottom + 1) * tileSize
    )

    return Pair(topLeftCalibrationPoint, bottomRightCalibrationPoint)
}

private data class LevelArea(
    val colLeft: Int,
    val rowTop: Int,
    val colRight: Int,
    val rowBottom: Int
)

private fun getLevelArea(
    level: Int,
    XLeft: Double,
    YTop: Double,
    XRight: Double,
    YBottom: Double
): LevelArea {
    val tileSize = getTileInMetersForZoom(level)
    val colLeft = floor((XLeft - X0) / tileSize).toInt()
    val rowTop = floor((Y0 - YTop) / tileSize).toInt()
    val colRight = ceil((XRight - X0) / tileSize).toInt()
    val rowBottom = ceil((Y0 - YBottom) / tileSize).toInt()
    return LevelArea(colLeft, rowTop, colRight, rowBottom)
}

/**
 * At level 0, a WMTS map is a square of 2 * |X0| side length (in meters).
 * And at this level, it is made of 1 tile.
 * The resolution is expressed in meters
 */
private fun getTileInMetersForZoom(level: Int): Double {
    return 2 * abs(X0) / 2.0.pow(level.toDouble())
}
