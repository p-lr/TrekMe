package com.peterlaurence.trekadvisor.core.mapsource.wmts

import kotlin.coroutines.experimental.buildIterator

data class Tile(val level: Int, val row: Int, val col: Int)

/**
 * At level 0, an IGN map (which use WebMercator projection) is contained in a single tile of
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
 * This function builds the map as an [Iterable] of [Tile] which contain the area defined by the two
 * supplied points. The build method is:
 *
 * * Start by finding the tiles at min level that overlap the area.
 * * Find the tiles at higher levels that match exactly the the area underneath the tiles at min level
 *
 * This build method has the advantage of producing a map whose levels cover the same area. But it
 * has a drawback. If the min level is too small, the tile size in meters at this level is high and
 * potentially a large unwanted area will be covered by this level and hence all higher levels. This
 * can lead to downloading a lot of unwanted tiles.
 *
 * A rule of thumb is to set the min level to 12 or higher, and max level no more than 18.
 */
fun getTileIterable(levelMin: Int, levelMax: Int, XLeft: Double, YTop: Double, XRight: Double, YBottom: Double): Iterable<Tile> {
    return Iterable {
        buildIterator {
            println(levelMin)
            val tileSize = getTileInMetersForZoom(levelMin)
            var colLeft = Math.floor((XLeft - X0) / tileSize).toInt()
            var rowTop = Math.floor((Y0 - YTop) / tileSize).toInt()
            var colRight = Math.ceil((XRight - X0) / tileSize).toInt()
            var rowBottom = Math.ceil((Y0 - YBottom) / tileSize).toInt()
            for (i in rowTop..rowBottom) {
                for (j in colLeft..colRight) {
                    yield(Tile(levelMin, i, j))
                }
            }

            for (level in (levelMin + 1)..levelMax) {
                println(level)
                colLeft *= 2
                rowTop *= 2
                colRight = (colRight + 1) * 2 - 1
                rowBottom = (rowBottom + 1) * 2 - 1
                for (i in rowTop..rowBottom) {
                    for (j in colLeft..colRight) {
                        yield(Tile(level, i, j))
                    }
                }
            }
        }
    }
}

/**
 * At level 0, an IGN map is a square of 2 * |X0| side length (in meters).
 * And at this level, it is made of 1 tile.
 * The resolution is expressed in meters
 */
private fun getTileInMetersForZoom(level: Int): Double {
    return 2 * Math.abs(X0) / Math.pow(2.0, level.toDouble())
}

const val X0 = -20037508.3427892476
const val Y0 = -X0



