package com.peterlaurence.mapview.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.peterlaurence.mapview.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

class TileCanvasViewModel(private val scope: CoroutineScope, tileSize: Int,
                          tileStreamProvider: TileStreamProvider): CoroutineScope by scope {
    private val tilesToRender = MutableLiveData<List<Tile>>()

    private val tileProvider = TileProviderImpl(BitmapPool(), tileSize)
    private val visibleTileLocationsChannel = Channel<List<TileLocation>>(capacity = Channel.CONFLATED)
    private val tilesOutput = Channel<Tile>(capacity = Channel.UNLIMITED)

    init {
        collectTiles(visibleTileLocationsChannel, tilesOutput, tileProvider, tileStreamProvider)
        consumeTiles(tilesOutput)
    }

    fun getTilesToRender() : LiveData<List<Tile>> {
        return tilesToRender
    }

    fun setVisibleTiles(visibleTiles: VisibleTiles) {
        val locations = visibleTiles.toTileLocations()
        visibleTileLocationsChannel.offer(locations)
    }

    private fun CoroutineScope.consumeTiles(tileChannel: ReceiveChannel<Tile>) = launch {
        for (tile in tileChannel) {
            println("received tile ${tile.zoom}-${tile.row}-${tile.col}")
        }
    }

    private fun VisibleTiles.toTileLocations(): List<TileLocation> {
        return (rowBottom..rowTop).map { row ->
            (colLeft..colRight).map { col ->
                TileLocation(level, row, col)
            }
        }.flatten()
    }
}