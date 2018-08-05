package com.peterlaurence.trekadvisor.core.providers.urltilebuilder

interface UrlTileBuilder {
    fun build(level: Int, row: Int, col: Int): String
}