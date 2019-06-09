package com.peterlaurence.mapview.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.peterlaurence.mapview.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

/**
 * The view-model which contains all the logic related to [Tile] management.
 * It defers [Tile] loading to [tileCollector].
 */
class TileCanvasViewModel(private val scope: CoroutineScope, tileSize: Int,
                          tileStreamProvider: TileStreamProvider): CoroutineScope by scope {
    private val tilesToRenderLiveData = MutableLiveData<List<Tile>>()

    private val bitmapPool = BitmapPool()
    private val tileProvider = TileProviderImpl(bitmapPool, tileSize)
    private val visibleTileLocationsChannel = Channel<List<TileLocation>>(capacity = Channel.CONFLATED)
    private val tilesOutput = Channel<Tile>(capacity = Channel.UNLIMITED)

    private lateinit var lastVisible: VisibleTiles
    private var tilesToRender = mutableListOf<Tile>()

    init {
        collectTiles(visibleTileLocationsChannel, tilesOutput, tileProvider, tileStreamProvider)
        consumeTiles(tilesOutput)
    }

    fun getTilesToRender() : LiveData<List<Tile>> {
        return tilesToRenderLiveData
    }

    fun setVisibleTiles(visibleTiles: VisibleTiles) {
        val locations = visibleTiles.toTileLocations()
        visibleTileLocationsChannel.offer(locations)

        lastVisible = visibleTiles
        processNewVisibleTiles(visibleTiles)

        render()
    }

    private fun CoroutineScope.consumeTiles(tileChannel: ReceiveChannel<Tile>) = launch {
        for (tile in tileChannel) {
            if (lastVisible.contains(tile)) {
                tilesToRender.add(tile)
                render()
            }
        }
    }

    private fun VisibleTiles.toTileLocations(): List<TileLocation> {
        return (rowBottom..rowTop).map { row ->
            (colLeft..colRight).map { col ->
                TileLocation(level, row, col)
            }
        }.flatten()
    }

    private fun VisibleTiles.contains(tile: Tile): Boolean {
        return tile.col in colLeft..colRight && tile.row in rowTop..rowBottom
    }

    /**
     * Each tile we get a new [VisibleTiles], remove all [Tile] from [tilesToRender] which aren't
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