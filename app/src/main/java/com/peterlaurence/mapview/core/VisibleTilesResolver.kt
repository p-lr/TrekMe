package com.peterlaurence.mapview.core


class VisibleTilesResolver(private val levelCount: Int, fullWidth: Int, fullHeight: Int, tileSize: Int = 256) {
    private var scale = 1.0f
    private var currentLevel = levelCount - 1

    fun setScale(scale: Float) {
        this.scale = scale

        /* Update current level */
        currentLevel = getLevel(scale)
    }

    fun getCurrentLevel(): Int {
        return currentLevel
    }

    /**
     * Returns the level an entire value belonging to [0 ; [levelCount] - 1]
     */
    private fun getLevel(scale: Float): Int {
        /* This value can ve negative */
        val partialLevel =  levelCount - 1 + Math.log(scale.toDouble()) / Math.log(2.0)

        /* The level can't be greater than levelCount - 1.0 */
        val capedLevel = Math.min(partialLevel, levelCount - 1.0)

        /* The level can't be lower than 0 */
        return Math.ceil(Math.max(capedLevel, 0.0)).toInt()
    }
}