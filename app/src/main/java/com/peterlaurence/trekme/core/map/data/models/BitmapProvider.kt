package com.peterlaurence.trekme.core.map.data.models

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.peterlaurence.trekme.core.map.domain.models.OutOfBounds
import com.peterlaurence.trekme.core.map.domain.models.TileStream
import com.peterlaurence.trekme.core.map.domain.models.TileStreamProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull


/**
 * From a [TileStreamProvider], and eventually bitmap options, get [Bitmap] from tile coordinates.
 * Attempts up to [maxRetry] to fetch a tile.
 */
class BitmapProvider(
    private val tileStreamProvider: TileStreamProvider,
    options: BitmapFactory.Options? = null
) {
    private var bitmapLoadingOptions = options ?: BitmapFactory.Options()
    private val maxRetry = 5

    suspend fun getBitmap(row: Int, col: Int, zoomLvl: Int): Bitmap? = withContext(Dispatchers.IO) {
        var retry = 0
        var bitmap: Bitmap?
        do {
            bitmap = withTimeoutOrNull(1000) {
                runCatching {
                    when (val tileResult = tileStreamProvider.getTileStream(row, col, zoomLvl)) {
                        OutOfBounds -> {
                            retry = maxRetry // leave the loop
                            return@withTimeoutOrNull null
                        }

                        is TileStream -> {
                            BitmapFactory.decodeStream(
                                tileResult.tileStream,
                                null,
                                bitmapLoadingOptions
                            )
                        }
                    }
                }.getOrNull()
            }
            if (bitmap == null) retry++
        } while (retry < maxRetry && bitmap == null)
        bitmap
    }

    fun setBitmapOptions(options: BitmapFactory.Options) {
        bitmapLoadingOptions = options
    }
}


