package com.peterlaurence.mapview.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.peterlaurence.mapview.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * The view-model which contains all the logic related to [Tile] management.
 * It defers [Tile] loading to [tileCollector].
 */
class TileCanvasViewModel(private val scope: CoroutineScope, tileSize: Int,
                          tileStreamProvider: TileStreamProvider) : CoroutineScope by scope {
    private val tilesToRenderLiveData = MutableLiveData<List<Tile>>()

    private val bitmapPool = BitmapPool()
    private val visibleTileLocationsChannel = Channel<List<TileLocation>>(capacity = Channel.CONFLATED)
    private val tilesOutput = Channel<Tile>(capacity = Channel.UNLIMITED)

    /**
     * A [Flow] of [Bitmap] that is guarantied to be collected on Main thread. But other flows can
     * collect it from other coroutines that are dispatched on other threads. It's a simple way to
     * share data between coroutines in thread safe way, using cold flows.
     */
    @FlowPreview
    private val bitmapFlow: Flow<Bitmap> = flow {
        val bitmapForPool = bitmapPool.getBitmap()
        val bitmap = if (bitmapForPool == null || bitmapForPool.isRecycled) {
            Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.RGB_565)
        } else {
            bitmapForPool
        }
        emit(bitmap)
    }.flowOn(Dispatchers.Main)

    private lateinit var lastVisible: VisibleTiles
    private var tilesToRender = mutableListOf<Tile>()

    init {
        collectTiles(visibleTileLocationsChannel, tilesOutput, tileStreamProvider, bitmapFlow)
        consumeTiles(tilesOutput)
    }

    fun getTilesToRender(): LiveData<List<Tile>> {
        return tilesToRenderLiveData
    }

    fun setVisibleTiles(visibleTiles: VisibleTiles) {
        val locations = visibleTiles.toTileLocations()
        visibleTileLocationsChannel.offer(locations)

        lastVisible = visibleTiles
        processNewVisibleTiles(visibleTiles)

        render()
    }

    /**
     * For each [Tile] received, add it to the list of tiles to render if it's visible. Otherwise,
     * add the corresponding Bitmap to the [bitmapPool].
     */
    private fun CoroutineScope.consumeTiles(tileChannel: ReceiveChannel<Tile>) = launch {
        for (tile in tileChannel) {
            if (lastVisible.contains(tile)) {
                tilesToRender.add(tile)
                render()
            } else {
                bitmapPool.putBitmap(tile.bitmap)
            }
        }
    }

    private fun VisibleTiles.toTileLocations(): List<TileLocation> {
        return (rowTop..rowBottom).map { row ->
            (colLeft..colRight).map { col ->
                TileLocation(level, row, col)
            }
        }.flatten()
    }

    private fun VisibleTiles.contains(tile: Tile): Boolean {
        return level == tile.zoom && tile.col in colLeft..colRight && tile.row in rowTop..rowBottom
    }

    /**
     * Each time we get a new [VisibleTiles], remove all [Tile] from [tilesToRender] which aren't
     * visible and put their bitmap into the pool.
     */
    private fun processNewVisibleTiles(visibleTiles: VisibleTiles) {
        tilesToRender = tilesToRender.filter { tile ->
            visibleTiles.contains(tile).also { inside ->
                if (!inside) {
                    bitmapPool.putBitmap(tile.bitmap)
                }
            }
        }.toMutableList()
    }

    /**
     * Post a new value to the observable. The view should update its UI.
     */
    private fun render() {
        tilesToRenderLiveData.postValue(tilesToRender)
    }
}