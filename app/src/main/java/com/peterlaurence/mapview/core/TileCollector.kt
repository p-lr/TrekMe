package com.peterlaurence.mapview.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

const val N_WORKERS = 4

/**
 * @param [visibleTiles] channel of [VisibleTiles], which capacity should be [Channel.CONFLATED].
 */
fun CoroutineScope.processVisibleTiles(visibleTiles: ReceiveChannel<VisibleTiles>) {
    val tilesToDownload = Channel<Tile>(capacity = Channel.UNLIMITED)
    val tilesDownloaded = Channel<Tile>(capacity = Channel.UNLIMITED)

    repeat(N_WORKERS) { worker(tilesToDownload, tilesDownloaded) }
    tileCollector(visibleTiles, tilesToDownload, tilesDownloaded)
}

fun CoroutineScope.worker(tilesToDownload: ReceiveChannel<Tile>,
                          tilesDownloaded: SendChannel<Tile>) = launch {
    for (tile in tilesToDownload) {
        // TODO : download tile, use the tileStreamProvider
        tilesDownloaded.send(tile)
    }
}

fun CoroutineScope.tileCollector(visibleTiles: ReceiveChannel<VisibleTiles>,
                                 tilesToDownload: SendChannel<Tile>,
                                 tilesDownloaded: ReceiveChannel<Tile>) = launch {

    var lastVisibleTiles = VisibleTiles(-1, 0, 0, 0, 0)
    val tilesCollected = mutableListOf<Tile>()

    while (true) {
        select<Unit> {
            visibleTiles.onReceive {
                dispatchTiles(it, tilesToDownload)

                /* Only keep tiles that are visible */
                if (lastVisibleTiles != it) {
                    tilesCollected.keepOnlyThoseInside(it)
                }

                lastVisibleTiles = it
            }

            tilesDownloaded.onReceive {
                if (it.isInside(lastVisibleTiles)) {
                    tilesCollected.add(it)
                }
            }
        }
    }
}

/**
 * If a tile is already stored inside the LRU cache, get it from there.
 */
private suspend fun dispatchTiles(visibleTiles: VisibleTiles,
                                  tilesToDownload: SendChannel<Tile>): List<Tile> {
    for (location in visibleTiles.toTileLocations()) {
        // if is already inside LRU cache, get it from there
        // else, send them to the tilesToDownload channel
    }
    TODO()
}

private fun VisibleTiles.toTileLocations(): List<TileLocation> {
    // simply convert to a tile list
    TODO()
}

private fun Tile.isInside(visibleTiles: VisibleTiles): Boolean {
    TODO()
}

private fun MutableList<Tile>.keepOnlyThoseInside(visibleTiles: VisibleTiles): List<Tile> = apply {
    TODO()
}

data class TileLocation(val zoom: Int, val row: Int, val col: Int)

interface TileProvider {
    fun getTile(zoom: Int, row: Int, col: Int): Tile
}