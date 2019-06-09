package com.peterlaurence.mapview.view

import android.content.Context
import android.graphics.Canvas
import android.view.View
import com.peterlaurence.mapview.core.Tile
import com.peterlaurence.mapview.core.VisibleTilesResolver
import com.peterlaurence.mapview.viewmodel.TileCanvasViewModel

/**
 * This is the view where all tiles are drawn into.
 *
 * @author peterLaurence on 02/06/2019
 */
class TileCanvasView(ctx: Context, viewModel: TileCanvasViewModel,
                     private val visibleTilesResolver: VisibleTilesResolver) : View(ctx) {
    private var scale = 1f

    var tilesToRender = listOf<Tile>()

    init {
        setWillNotDraw(false)

        viewModel.getTilesToRender().observeForever {
            tilesToRender = it
            invalidate()
        }
    }

    fun setScale(scale: Float) {
        this.scale = scale
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.scale(scale, scale)
        drawTiles(canvas)
        canvas.restore()
    }

    private fun drawTiles(canvas: Canvas) {
        if (tilesToRender.isEmpty()) return

        for (tile in tilesToRender) {
            val scaleForLevel = visibleTilesResolver.getScaleForLevel(tilesToRender.first().zoom) ?: continue
            val x = tile.col / scaleForLevel
            val y = tile.row / scaleForLevel
            canvas.drawBitmap(tile.bitmap, x, y, null)
        }
    }
}