package com.peterlaurence.mapview.core

/**
 * Resolves the visible tiles
 *
 * @param levelCount Number of levels
 * @param fullWidth Width of the map at scale 1.0f
 * @param fullHeight Height of the map at scale 1.0f
 * @param magnifyingFactor Alters the level at which tiles are picked for a given scale. By default,
 * the level immediately higher (in index) is picked, to avoid sub-sampling. This corresponds to a
 * [magnifyingFactor] of 0. The value 1 will result in picking the current level at a given scale,
 * which will be at a relative scale between 1.0 and 2.0
 */
class VisibleTilesResolver (private val levelCount: Int, private val fullWidth: Int,
                            private val fullHeight: Int, private val tileSize: Int = 256,
                            private val magnifyingFactor: Int = 0) {

    private var scale: Float = 1.0f
    private var currentLevel = levelCount - 1

    /**
     * Last level is at scale 1.0f, others are at scale 1.0 / power_of_2
     */
    private val scaleForLevel = (0 until levelCount).associateWith {
        (1.0 / Math.pow(2.0, (levelCount - it - 1).toDouble())).toFloat()
    }

    fun setScale(scale: Float) {
        this.scale = scale

        /* Update current level */
        currentLevel = getLevel(scale, magnifyingFactor)
    }

    fun getCurrentLevel(): Int {
        return currentLevel
    }

    /**
     * Returns the level an entire value belonging to [0 ; [levelCount] - 1]
     */
    private fun getLevel(scale: Float, magnifyingFactor: Int = 0): Int {
        /* This value can be negative */
        val partialLevel = levelCount - 1 - magnifyingFactor +
                Math.log(scale.toDouble()) / Math.log(2.0)

        /* The level can't be greater than levelCount - 1.0 */
        val capedLevel = Math.min(partialLevel, levelCount - 1.0)

        /* The level can't be lower than 0 */
        return Math.ceil(Math.max(capedLevel, 0.0)).toInt()
    }

    /**
     * Get the [VisibleTiles], given the visible area in pixels.
     *
     * @param viewport The [Viewport] which represents the visible area. Its values depend on the
     * scale.
     */
    fun getVisibleTiles(viewport: Viewport): VisibleTiles {
        val scaledTileSize = tileSize.toDouble() * getRelativeScale()
        val scaledWidth = fullWidth * scale
        val scaledHeight = fullHeight * scale

        val top = Math.max(viewport.top, 0)
        val left = Math.max(viewport.left, 0)
        val right = Math.min(viewport.right, scaledWidth.toInt())
        val bottom = Math.min(viewport.bottom, scaledHeight.toInt())

        val colLeft = Math.floor(left / scaledTileSize).toInt()
        val rowTop = Math.floor(top / scaledTileSize).toInt()
        val colRight = Math.ceil(right / scaledTileSize).toInt() - 1
        val rowBottom = Math.ceil(bottom / scaledTileSize).toInt() - 1

        return VisibleTiles(currentLevel, colLeft, rowTop, colRight, rowBottom)
    }

    private fun getRelativeScale(): Float {
        return (scaleForLevel[currentLevel] ?: throw AssertionError()) / scale
    }
}

/**
 * @param level 0-based level index
 */
data class VisibleTiles(var level: Int, val colLeft: Int, val rowTop: Int, val colRight: Int,
                        val rowBottom: Int)