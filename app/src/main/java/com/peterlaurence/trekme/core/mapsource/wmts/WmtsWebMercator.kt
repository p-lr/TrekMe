package com.peterlaurence.trekme.core.mapsource.wmts

import com.peterlaurence.trekme.core.map.entity.MapGson.Calibration.CalibrationPoint
import kotlin.math.*

data class Tile(val level: Int, val row: Int, val col: Int, val indexLevel: Int, val indexRow: Int,
                val indexCol: Int)

data class Point(val X: Double, val Y: Double)
data class MapSpec(val levelMin: Int, val levelMax: Int, val mapWidthPx: Int, val mapHeightPx: Int,
                   val tileSequence: Sequence<Tile>, val calibrationPoints: Pair<CalibrationPoint, CalibrationPoint>,
                   val tileSize: Int)

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
 *    |         IGN map        |
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
 * * Start by finding the tiles at min level that overlap the area.
 * * Find the tiles at higher levels that match exactly the the area underneath the tiles at min level
 *
 * This algorithm has the advantage of producing a map whose levels cover the same area. But it
 * has a drawback. If the min level is too small, the tile size in meters at this level is high and
 * potentially a large unwanted area will be covered by this level and hence all higher levels. This
 * can lead to downloading a lot of unwanted tiles.
 *
 * A rule of thumb is to set the min level to 12 or higher, and max level no more than 17.
 *
 * Calibration points are set on top-left and bottom-right corners. They are computed with the tiles
 * of the min level since this is the level used for getting the boundaries of the map.
 *
 * @param point1 A [Point] at any corner
 * @param point2 A [Point] at the opposite corner of [point1]
 */
fun getMapSpec(levelMin: Int, levelMax: Int, point1: Point, point2: Point): MapSpec {
    val (XLeft, YTop, XRight, YBottom) = orderCoordinates(point1, point2)

    val tileSequence = getTileSequence(levelMin, levelMax, XLeft, YTop, XRight, YBottom)
    val calibrationPoints = getCalibrationPoints(levelMin, XLeft, YTop, XRight, YBottom)
    val mapSize = getMapSize(levelMin, levelMax, XLeft, YTop, XRight, YBottom)

    return MapSpec(levelMin, levelMax, mapSize.widthPx, mapSize.heightPx, tileSequence, calibrationPoints, TILE_SIZE_PX)
}

fun getNumberOfTiles(levelMin: Int, levelMax: Int, point1: Point, point2: Point): Long {
    val (XLeft, YTop, XRight, YBottom) = orderCoordinates(point1, point2)

    return getNumberOfTiles(levelMin, levelMax, XLeft, YTop, XRight, YBottom)
}

/**
 * One transaction is equivalent to [TILES_PER_TRANSACTION] tiles.
 */
fun Long.toTransactionsNumber(): Long {
    return ceil(this / TILES_PER_TRANSACTION).toLong()
}

/**
 * Given a number of tiles, get the resulting approximate size in Mo.
 * [TILE_SIZE_IN_MO] is an average size.
 */
fun Long.toSizeInMo(): Long {
    return (this * TILE_SIZE_IN_MO).toLong()
}


private data class TopLeftToBottomRight(val XLeft: Double, val YTop: Double, val XRight: Double, val YBottom: Double)

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
private class LevelBuilder(levelMin: Int, levelMax: Int, XLeft: Double, YTop: Double, XRight: Double, YBottom: Double) {
    private val levelAreaMap = mutableMapOf<Int, LevelArea>()

    init {
        val minArea = getLevelArea(levelMin, XLeft, YTop, XRight, YBottom)
        levelAreaMap[levelMin] = minArea

        var (colLeft, rowTop, colRight, rowBottom) = minArea
        for (level in (levelMin + 1)..levelMax) {
            colLeft *= 2
            rowTop *= 2
            colRight = (colRight + 1) * 2 - 1
            rowBottom = (rowBottom + 1) * 2 - 1
            levelAreaMap[level] = LevelArea(colLeft, rowTop, colRight, rowBottom)
        }
    }

    fun getLevelAreaForLevel(level: Int): LevelArea? {
        return levelAreaMap[level]
    }
}

private fun getTileSequence(levelMin: Int, levelMax: Int, XLeft: Double, YTop: Double, XRight: Double, YBottom: Double): Sequence<Tile> {
    val levelBuilder = LevelBuilder(levelMin, levelMax, XLeft, YTop, XRight, YBottom)

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

private fun getNumberOfTiles(levelMin: Int, levelMax: Int, XLeft: Double, YTop: Double, XRight: Double, YBottom: Double): Long {
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
private fun getMapSize(levelMin: Int, levelMax: Int, XLeft: Double, YTop: Double, XRight: Double, YBottom: Double): MapSize {
    val levelBuilder = LevelBuilder(levelMin, levelMax, XLeft, YTop, XRight, YBottom)

    val levelMaxArea = levelBuilder.getLevelAreaForLevel(levelMax) ?: return MapSize(0, 0)
    return with(levelMaxArea) {
        MapSize((colRight - colLeft + 1) * TILE_SIZE_PX, (rowBottom - rowTop + 1) * TILE_SIZE_PX)
    }
}

private data class MapSize(val widthPx: Int, val heightPx: Int)

private fun getCalibrationPoints(level: Int, XLeft: Double, YTop: Double, XRight: Double, YBottom: Double):
        Pair<CalibrationPoint, CalibrationPoint> {
    val (colLeft, rowTop, colRight, rowBottom) = getLevelArea(level, XLeft, YTop, XRight, YBottom)
    val tileSize = getTileInMetersForZoom(level)

    val topLeftCalibrationPoint = CalibrationPoint()
    topLeftCalibrationPoint.x = 0.0
    topLeftCalibrationPoint.y = 0.0
    topLeftCalibrationPoint.proj_x = colLeft * tileSize + X0
    topLeftCalibrationPoint.proj_y = Y0 - rowTop * tileSize

    val bottomRightCalibrationPoint = CalibrationPoint()
    bottomRightCalibrationPoint.x = 1.0
    bottomRightCalibrationPoint.y = 1.0
    bottomRightCalibrationPoint.proj_x = (colRight + 1) * tileSize + X0
    bottomRightCalibrationPoint.proj_y = Y0 - (rowBottom + 1) * tileSize

    return Pair(topLeftCalibrationPoint, bottomRightCalibrationPoint)
}

private data class LevelArea(val colLeft: Int, val rowTop: Int, val colRight: Int, val rowBottom: Int)

private fun getLevelArea(level: Int, XLeft: Double, YTop: Double, XRight: Double, YBottom: Double): LevelArea {
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

const val X0 = -20037508.3427892476
const val Y0 = -X0
const val TILES_PER_TRANSACTION = 15.981973 // only meaningful for France IGN
const val TILE_SIZE_IN_MO = 0.0169
const val TILE_SIZE_PX = 256



