package com.peterlaurence.mapview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import android.view.animation.AnimationUtils
import com.peterlaurence.mapview.core.Tile
import com.peterlaurence.mapview.core.VisibleTilesResolver
import com.peterlaurence.mapview.viewmodel.TileCanvasViewModel
import kotlin.math.min

/**
 * This is the view where all tiles are drawn into.
 *
 * @author peterLaurence on 02/06/2019
 */
class TileCanvasView(ctx: Context, viewModel: TileCanvasViewModel,
                     private val tileSize: Int,
                     private val visibleTilesResolver: VisibleTilesResolver) : View(ctx) {
    private var scale = 1f
    private var lastTime: Long = -1
    private val alphaTickDurationInMs = 200

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
        invalidate()
    }

    fun shouldRequestLayout() {
        requestLayout()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.scale(scale, scale)
        drawTiles(canvas)
        canvas.restore()
    }

    /**
     * Draw tiles, while optimizing alpha-related computations (the alpha progress is indeed
     * computed at most once for each [drawTiles] call). But each tile has its own alpha value.
     */
    private fun drawTiles(canvas: Canvas) {
        if (tilesToRender.isEmpty()) return

        var alphaProgressComputed = false
        var alphaProgress = 0f
        var needsAnotherPass = false
        for (tile in tilesToRender) {
            val scaleForLevel = visibleTilesResolver.getScaleForLevel(tile.zoom)
                    ?: continue
            val tileScaled = (tileSize / scaleForLevel).toInt()
            val l = tile.col * tileScaled
            val t = tile.row * tileScaled
            val r = l + tileScaled
            val b = t + tileScaled
            val dest = Rect(l, t, r, b)

            /* If a tile has a paint, compute only once the alphaProgress for this loop */
            val paint = tile.paint?.also {
                if (!alphaProgressComputed && it.alpha < 255) {
                    alphaProgress = computeAlphaProgress()
                    alphaProgressComputed = true
                }
                it.updateAlpha(alphaProgress).let { a ->
                    needsAnotherPass = needsAnotherPass || (a < 255)
                }
            }

            canvas.drawBitmap(tile.bitmap, null, dest, paint)
        }

        /* If at least one tile wasn't fully drawn (alpha < 255), redraw */
        if (needsAnotherPass) {
            invalidate()
        }
    }

    /**
     * Increase the alpha, but don't exceed 255.
     * @return its new value
     */
    private fun Paint.updateAlpha(alphaProgress: Float): Int {
        val newAlpha = alpha + (255 * alphaProgress).toInt()
        alpha = Math.min(255, newAlpha)
        return alpha
    }

    /**
     * Get the percent of elapsed time based on [alphaTickDurationInMs].
     */
    private fun computeAlphaProgress(): Float {
        val time = AnimationUtils.currentAnimationTimeMillis()
        val elapsed = if (lastTime != -1L) time - lastTime else 0
        lastTime = time
        return (elapsed.toFloat() / alphaTickDurationInMs)
    }
}