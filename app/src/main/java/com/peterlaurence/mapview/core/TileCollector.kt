package com.peterlaurence.mapview.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger


/**
 * The engine of the MapView. TODO: document this
 *
 * @author peterLaurence on 22/06/19
 */
class TileCollector(private val workerCount: Int) {
    private val dispatcher = createDispatcher(workerCount)

    /**
     * Sets up the tile collector machinery. The architecture is inspired from
     * [Kotlin Conf 2018](https://www.youtube.com/watch?v=a3agLJQ6vt8).
     * @param [tileSpecs] channel of [TileSpec], which capacity should be [Channel.CONFLATED].
     * @param [tilesOutput] channel of [Tile], which should be set as [Channel.RENDEZVOUS].
     */
    fun CoroutineScope.collectTiles(tileSpecs: ReceiveChannel<List<TileSpec>>,
                                    tilesOutput: SendChannel<Tile>,
                                    tileStreamProvider: TileStreamProvider,
                                    bitmapFlow: Flow<Bitmap>) {
        val tilesToDownload = Channel<TileStatus>(capacity = Channel.RENDEZVOUS)
        val tilesDownloadedFromWorker = Channel<TileStatus>(capacity = Channel.UNLIMITED)

        repeat(workerCount) { worker(tilesToDownload, tilesDownloadedFromWorker, tilesOutput, tileStreamProvider, bitmapFlow) }
        tileCollector(tileSpecs, tilesToDownload, tilesDownloadedFromWorker)
    }

    private fun CoroutineScope.worker(tilesToDownload: ReceiveChannel<TileStatus>,
                                      tilesDownloaded: SendChannel<TileStatus>,
                                      tilesOutput: SendChannel<Tile>,
                                      tileStreamProvider: TileStreamProvider,
                                      bitmapFlow: Flow<Bitmap>) = launch(dispatcher) {

        val bitmapLoadingOptions = BitmapFactory.Options()
        bitmapLoadingOptions.inPreferredConfig = Bitmap.Config.RGB_565

        for (tileStatus in tilesToDownload) {
            /* If it was cancelled, do nothing and send back the TileSpec as is */
            if (tileStatus.cancelled) {
                tilesDownloaded.send(tileStatus)
                continue
            }

            val spec = tileStatus.spec

            val i = tileStreamProvider.getTileStream(spec.row, spec.col, spec.zoom)

            if (spec.subSample > 0) {
                bitmapLoadingOptions.inBitmap = null
                bitmapLoadingOptions.inScaled = true
                bitmapLoadingOptions.inSampleSize = spec.subSample
            } else {
                bitmapLoadingOptions.inScaled = false
                bitmapLoadingOptions.inBitmap = bitmapFlow.single()
                bitmapLoadingOptions.inSampleSize = 0
            }

            try {
                val bitmap = BitmapFactory.decodeStream(i, null, bitmapLoadingOptions) ?: continue
                val tile = Tile(spec.zoom, spec.row, spec.col, spec.subSample).apply {
                    this.bitmap = bitmap
                }
                tilesOutput.send(tile)
            } catch (e: OutOfMemoryError) {
                // no luck
            } catch (e: Exception) {
                // maybe retry
            } finally {
                tilesDownloaded.send(tileStatus)
                i?.close()
            }
        }
    }

    private fun CoroutineScope.tileCollector(tileSpecs: ReceiveChannel<List<TileSpec>>,
                                             tilesToDownload: SendChannel<TileStatus>,
                                             tilesDownloadedFromWorker: ReceiveChannel<TileStatus>) = launch {

        val tilesBeingProcessed = mutableListOf<TileStatus>()

        while (true) {
            select<Unit> {
                tileSpecs.onReceive {
                    for (loc in it) {
                        if (!tilesBeingProcessed.any { status -> status.spec == loc }) {
                            /* Add it to the list of locations being processed */
                            val status = TileStatus(loc)
                            tilesBeingProcessed.add(status)

                            /* Now download the tile */
                            tilesToDownload.send(status)
                        }
                    }
                    for (status in tilesBeingProcessed) {
                        if (!it.contains(status.spec)) {
                            status.cancelled = true
                        }
                    }
                }

                tilesDownloadedFromWorker.onReceive {
                    tilesBeingProcessed.remove(it)
                }
            }
        }
    }

    data class TileStatus(val spec: TileSpec, @Volatile var cancelled: Boolean = false)

    /**
     * We build our own coroutine dispatcher, as we want to set the min priority to each thread of the
     * pool.
     */
    private fun createDispatcher(workerCount: Int): ExecutorCoroutineDispatcher {
        val threadId = AtomicInteger()
        return Executors.newFixedThreadPool(workerCount) {
            Thread(it, "TileCollector-worker-${threadId.incrementAndGet()}").apply {
                isDaemon = true
                priority = Thread.MIN_PRIORITY
                Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST)
            }
        }.asCoroutineDispatcher()
    }
}