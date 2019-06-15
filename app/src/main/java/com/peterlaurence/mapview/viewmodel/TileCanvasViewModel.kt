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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * The view-model which contains all the logic related to [Tile] management.
 * It defers [Tile] loading to [tileCollector].
 */
class TileCanvasViewModel(private val scope: CoroutineScope, tileSize: Int,
                          tileStreamProvider: TileStreamProvider) : CoroutineScope by scope {
    private val tilesToRenderLiveData = MutableLiveData<List<Tile>>()

    private val bitmapPool = BitmapPool()
    private val visibleTileLocationsChannel = Channel<List<TileSpec>>(capacity = Channel.CONFLATED)
    private val tilesOutput = Channel<Tile>(capacity = Channel.UNLIMITED)

    /**
     * A [Flow] of [Bitmap] that first collects from the [bitmapPool] on the Main thread. If the
     * pool was empty, a new [Bitmap] is allocated from the calling thread. It's a simple way to
     * share data between coroutines in a thread safe way, using cold flows.
     */
    @FlowPreview
    private val bitmapFlow: Flow<Bitmap> = flow {
        val bitmap = bitmapPool.getBitmap()
        emit(bitmap)
    }.flowOn(Dispatchers.Main).map {
        it ?: Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.RGB_565)
    }

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
        collectNewTiles(visibleTiles)

        lastVisible = visibleTiles
        evictTiles(visibleTiles)

        render()
    }

    private fun collectNewTiles(visibleTiles: VisibleTiles) {
        val locations = visibleTiles.toTileSpecs()
        val locationWithoutTile = locations.filterNot { loc ->
            tilesToRender.any {
                it.sameSpecAs(loc)
            }
        }
        visibleTileLocationsChannel.offer(locationWithoutTile)
    }

    /**
     * For each [Tile] received, add it to the list of tiles to render if it's visible. Otherwise,
     * add the corresponding Bitmap to the [bitmapPool].
     */
    private fun CoroutineScope.consumeTiles(tileChannel: ReceiveChannel<Tile>) = launch {
        for (tile in tileChannel) {
            if (lastVisible.contains(tile)) {
                if (!tilesToRender.hasAlready(tile)) {
                    tilesToRender.add(tile)
                } else {
                    bitmapPool.putBitmap(tile.bitmap)
                }
                render()
            } else {
                bitmapPool.putBitmap(tile.bitmap)
            }
        }
    }

    private fun VisibleTiles.toTileSpecs(): List<TileSpec> {
        return (rowTop..rowBottom).map { row ->
            (colLeft..colRight).map { col ->
                TileSpec(level, row, col, subSample)
            }
        }.flatten()
    }

    private fun VisibleTiles.contains(tile: Tile): Boolean {
        return level == tile.zoom && subSample == tile.subSample && tile.col in colLeft..colRight
                && tile.row in rowTop..rowBottom
    }

    private fun VisibleTiles.count(): Int {
        return (rowBottom - rowTop + 1) * (colRight - colLeft + 1)
    }

    private fun List<Tile>.countTilesAtLevelAndSubSample(zoom: Int, subSample: Int): Int {
        var i = 0
        forEach {
            if (it.zoom == zoom && it.subSample == subSample) i++
        }
        return i
    }

    /**
     * Each time we get a new [VisibleTiles], remove all [Tile] from [tilesToRender] which aren't
     * visible or that aren't needed anymore and put their bitmap into the pool.
     */
    private fun evictTiles(visibleTiles: VisibleTiles) {
        val currentLevel = visibleTiles.level
        val currentSubSample = visibleTiles.subSample

        /* First, remove tiles that aren't visible at current level */
        val iterator = tilesToRender.iterator()
        while (iterator.hasNext()) {
            val tile = iterator.next()
            if (tile.zoom == currentLevel && tile.subSample == visibleTiles.subSample && !visibleTiles.contains(tile)) {
                iterator.remove()
                bitmapPool.putBitmap(tile.bitmap)
            }
        }

        /* Now, deal with tiles of other levels */
        val tilesOutsideCurrLevel = tilesToRender.filter {
            it.zoom != currentLevel || it.subSample != currentSubSample
        }
        if (tilesOutsideCurrLevel.isNotEmpty()) {
            val currCount = tilesToRender.countTilesAtLevelAndSubSample(currentLevel, currentSubSample)
            if (currCount == visibleTiles.count()) {
                tilesOutsideCurrLevel.forEach {
                    bitmapPool.putBitmap(it.bitmap)
                }
                tilesToRender = tilesToRender.filter {
                    it.zoom == currentLevel && it.subSample == currentSubSample
                }.toMutableList()
            }
        }

        /* Lastly, tiles of current level should be at the end of the list to be rendered above */
        tilesToRender.sortBy {
            it.zoom == currentLevel
        }
    }

    /**
     * Post a new value to the observable. The view should update its UI.
     */
    private fun render() {
        tilesToRenderLiveData.postValue(tilesToRender)
    }
}

private fun List<Tile>.hasAlready(tile: Tile) = any {
    it.zoom == tile.zoom && it.row == tile.row && it.col == tile.col && it.subSample == tile.subSample
}