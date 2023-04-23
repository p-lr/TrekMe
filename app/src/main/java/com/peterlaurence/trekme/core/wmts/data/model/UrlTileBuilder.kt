package com.peterlaurence.trekme.core.wmts.data.model

interface UrlTileBuilder {
    fun build(level: Int, row: Int, col: Int): String
}