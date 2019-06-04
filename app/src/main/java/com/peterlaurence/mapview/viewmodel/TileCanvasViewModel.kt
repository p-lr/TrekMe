package com.peterlaurence.mapview.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.peterlaurence.mapview.core.Tile

class TileCanvasViewModel {
    private val tilesToRender = MutableLiveData<List<Tile>>()

    fun getTilesToRender() : LiveData<List<Tile>> {
        return tilesToRender
    }

}