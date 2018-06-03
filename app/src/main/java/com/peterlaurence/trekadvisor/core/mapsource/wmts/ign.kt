package com.peterlaurence.trekadvisor.core.mapsource.wmts

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
 */
fun getTileIterator() {

}

const val X0 = -20037508.3427892476
const val Y0 = -X0

