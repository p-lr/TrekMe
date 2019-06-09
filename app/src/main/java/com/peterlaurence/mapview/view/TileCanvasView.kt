package com.peterlaurence.mapview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
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
                     private val tileSize: Int,
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
            val scaleForLevel = visibleTilesResolver.getScaleForLevel(tilesToRender.first().zoom)
                    ?: continue
            val tileScaled = (tileSize / scaleForLevel).toInt()
            val l = tile.col * tileScaled
            val t = tile.row * tileScaled
            val r = l + tileScaled
            val b = t + tileScaled
            val dest = Rect(l, t, r, b)

            canvas.drawBitmap(tile.bitmap, null, dest, null)
        }
    }
}