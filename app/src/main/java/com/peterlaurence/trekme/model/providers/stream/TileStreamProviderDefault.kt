package com.peterlaurence.trekme.model.providers.stream

import com.peterlaurence.trekme.core.map.TileStreamProvider
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.lang.Exception

class TileStreamProviderDefault(private val directory: File, private val extension: String): TileStreamProvider {

    override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
        val relativePathString = "$zoomLvl${File.separator}$row${File.separator}$col$extension"

        return try {
            FileInputStream(File(directory, relativePathString))
        } catch (e: Exception) {
            null
        }
    }
}