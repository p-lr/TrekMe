package com.peterlaurence.trekme.core.map.domain.models

import java.io.InputStream


interface TileStreamProvider {
    fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult
}

sealed class TileResult
data class TileStream(val tileStream: InputStream?) : TileResult()
object OutOfBounds : TileResult()


fun TileStreamProvider.withRetry(maxRetry: Int): TileStreamProvider {
    return object : TileStreamProvider {
        override fun getTileStream(row: Int, col: Int, zoomLvl: Int): TileResult {
            var retry = 0
            var result: TileResult
            do {
                result = when (val tileResult = this@withRetry.getTileStream(row, col, zoomLvl)) {
                    OutOfBounds -> {
                        retry = maxRetry // leave the loop
                        tileResult
                    }

                    is TileStream -> {
                        if (tileResult.tileStream == null) {
                            retry++
                        } else {
                            retry = maxRetry // leave the loop
                        }
                        tileResult
                    }
                }
            } while (retry < maxRetry)
            return result
        }
    }
}