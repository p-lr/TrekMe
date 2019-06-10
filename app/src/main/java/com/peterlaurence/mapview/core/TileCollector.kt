package com.peterlaurence.mapview.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * We build our own coroutine dispatcher, as we want to set the min priority to each thread of the
 * pool. Also, the size of the pool should be less than the number of cores (which is different from
 * [Dispatchers.Default] which uses exactly the number of cores).
 */
val threadId = AtomicInteger()
val nCores = Runtime.getRuntime().availableProcessors()
val nWorkers = nCores - 1
val dispatcher = Executors.newFixedThreadPool(nWorkers) {
    Thread(it, "TileCollector-worker-${threadId.incrementAndGet()}").apply {
        isDaemon = true
        priority = Thread.MIN_PRIORITY
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST)
    }
}.asCoroutineDispatcher()

/**
 * @param [visibleTileLocations] channel of [TileLocation], which capacity should be [Channel.CONFLATED].
 * @param [tilesOutput] channel of [Tile], which capacity should be [Channel.UNLIMITED]
 */
fun CoroutineScope.collectTiles(visibleTileLocations: ReceiveChannel<List<TileLocation>>,
                                tilesOutput: SendChannel<Tile>,
                                tileProvider: TileProvider,
                                tileStreamProvider: TileStreamProvider) {
    val tilesToDownload = Channel<TileSpec>(capacity = Channel.RENDEZVOUS)
    val tilesDownloadedFromWorker = Channel<TileSpec>(capacity = Channel.UNLIMITED)

    repeat(nWorkers) { worker(tilesToDownload, tilesDownloadedFromWorker, tileStreamProvider) }
    tileCollector(visibleTileLocations, tilesToDownload, tilesDownloadedFromWorker, tilesOutput,
            tileProvider)
}

private fun CoroutineScope.worker(tilesToDownload: ReceiveChannel<TileSpec>,
                                  tilesDownloaded: SendChannel<TileSpec>,
                                  tileStreamProvider: TileStreamProvider) = launch(dispatcher) {

    val bitmapLoadingOptions = BitmapFactory.Options()
    bitmapLoadingOptions.inPreferredConfig = Bitmap.Config.RGB_565

    for (tileSpec in tilesToDownload) {
        /* If it was cancelled, do nothing and send back the TileSpec as is */
        if (tileSpec.status.cancelled) {
            tilesDownloaded.send(tileSpec)
            continue
        }

        val tile = tileSpec.tile
        val i = tileStreamProvider.getTileStream(tile.row, tile.col, tile.zoom)
        bitmapLoadingOptions.inBitmap = tile.bitmap

        try {
            BitmapFactory.decodeStream(i, null, bitmapLoadingOptions)
            tilesDownloaded.send(tileSpec)
        } catch (e: OutOfMemoryError) {
            // no luck
        } catch (e: Exception) {
            // maybe retry
        }
    }
}

private fun CoroutineScope.tileCollector(tileLocations: ReceiveChannel<List<TileLocation>>,
                                         tilesToDownload: SendChannel<TileSpec>,
                                         tilesDownloadedFromWorker: ReceiveChannel<TileSpec>,
                                         tilesOutput: SendChannel<Tile>,
                                         tileProvider: TileProvider) = launch {

    val tilesBeingProcessed = mutableListOf<TileStatus>()

    while (true) {
        select<Unit> {
            tileLocations.onReceive {
                for (loc in it) {
                    if (!tilesBeingProcessed.any { status -> status.location == loc }) {
                        /* Add it to the list of locations being processed */
                        val status = TileStatus(loc)
                        tilesBeingProcessed.add(status)

                        /* Now download the tile */
                        val tile = tileProvider.getTile(loc.zoom, loc.row, loc.col)
                        tilesToDownload.send(TileSpec(status, tile))
                    }
                }
                for (status in tilesBeingProcessed) {
                    if (!it.contains(status.location)) {
                        status.cancelled = true
                    }
                }
            }

            tilesDownloadedFromWorker.onReceive {
                if (it.status.cancelled) {
                    tileProvider.recycleTile(it.tile)
                } else {
                    tilesOutput.send(it.tile)
                }

                /* Now remove the corresponding TileStatus from the list */
                tilesBeingProcessed.remove(it.status)
            }
        }
    }
}

data class TileLocation(val zoom: Int, val row: Int, val col: Int)

interface TileProvider {
    fun getTile(zoom: Int, row: Int, col: Int): Tile
    fun recycleTile(tile: Tile)
}

data class TileStatus(val location: TileLocation, @Volatile var cancelled: Boolean = false)
data class TileSpec(val status: TileStatus, val tile: Tile)