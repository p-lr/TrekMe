package com.peterlaurence.mapview.core

import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * Test the [collectTiles] engine. The following assertions are tested:
 * * The [TileProvider] should pick a [Bitmap] from the pool if possible
 * * If [TileSpec]s are send to the input channel, corresponding [Tile]s are received from the
 * output channel (from the [collectTiles] point of view).
 * * The [Bitmap] of the [Tile]s produced should be consistent with the output of the [TileProvider]
 */
@RunWith(RobolectricTestRunner::class)
class TileCollectorTest {

    private val tileSize = 256

    companion object {
        private var assetsDir: File? = null

        init {
            try {
                val mapviewDirURL = TileCollectorTest::class.java.classLoader!!.getResource("mapview")
                assetsDir = File(mapviewDirURL.toURI())
            } catch (e: Exception) {
                println("No mapview directory found.")
            }

        }
    }

    @Test
    fun fullTest() = runBlocking {
        assertNotNull(assetsDir)
        val imageFile = File(assetsDir, "10.jpg")
        assertTrue(imageFile.exists())


        /* Setup the channels */
        val visibleTileLocationsChannel = Channel<List<TileSpec>>(capacity = Channel.CONFLATED)
        val tilesOutput = Channel<Tile>(capacity = Channel.UNLIMITED)

        val pool = BitmapPool()
        val tileProvider = TileProviderImpl(pool, tileSize)

        val tileStreamProvider = object : TileStreamProvider {
            override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
                return FileInputStream(imageFile)
            }
        }

        val bitmapReference = tileProvider.getTile(0, 0, 0).bitmap

        fun CoroutineScope.consumeTiles(tileChannel: ReceiveChannel<Tile>) = launch {
            var cnt = 0
            for (tile in tileChannel) {
                println("received tile ${tile.zoom}-${tile.row}-${tile.col}")
                assertTrue(tile.bitmap.sameAs(bitmapReference))

                /* Add bitmap to the pool */
                if (tile.zoom == 0) {
                    pool.putBitmap(tile.bitmap)
                    cnt++
                    assertEquals(cnt, pool.size)
                }

                /* All bitmap from the last run should be out of the pool */
                if (tile.zoom == 1) {
                    assertEquals(0, pool.size)
                }
            }
        }

        val job = launch {
            collectTiles(visibleTileLocationsChannel, tilesOutput, tileProvider, tileStreamProvider)
            consumeTiles(tilesOutput)
        }

        launch {
            val locations1 = listOf(
                    TileSpec(0, 0, 0),
                    TileSpec(0, 1, 1),
                    TileSpec(0, 2, 1)
            )
            visibleTileLocationsChannel.send(locations1)
            delay(100)
            val locations2 = listOf(
                    TileSpec(1, 0, 0),
                    TileSpec(1, 1, 1),
                    TileSpec(1, 2, 1)
            )
            /* Bitmaps inside the pool should be used */
            visibleTileLocationsChannel.send(locations2)

            // wait a little, then cancel the job
            delay(1000)
            job.cancel()
        }
        Unit
    }
}