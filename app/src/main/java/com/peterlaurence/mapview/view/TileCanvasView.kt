package com.peterlaurence.mapview.view

import android.content.Context
import android.graphics.Canvas
import android.view.View

/**
 * This is the view where all tiles are drawn into.
 *
 * @author peterLaurence on 02/06/2019
 */
class TileCanvasView(ctx: Context) : View(ctx) {
    private var scale = 1f

    init {
        setWillNotDraw(false)
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

    }
}