package com.peterlaurence.trekme.core.map.data.dao

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.peterlaurence.trekme.core.map.data.MAP_FOLDER_NAME
import com.peterlaurence.trekme.core.map.data.models.MapFileBased
import com.peterlaurence.trekme.core.map.domain.dao.MapDownloadDao
import com.peterlaurence.trekme.core.map.domain.models.*
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.utils.createNomediaFile
import com.peterlaurence.trekme.core.wmts.domain.model.MapSpec
import com.peterlaurence.trekme.core.wmts.domain.model.Tile
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.map.data.models.BitmapProvider
import com.peterlaurence.trekme.core.map.data.models.makeTag
import com.peterlaurence.trekme.core.settings.Settings
import com.peterlaurence.trekme.core.wmts.domain.model.IgnClassic
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.IgnSpainData
import com.peterlaurence.trekme.core.wmts.domain.model.MapSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.OpenTopoMap
import com.peterlaurence.trekme.core.wmts.domain.model.OrdnanceSurveyData
import com.peterlaurence.trekme.core.wmts.domain.model.OsmAndHd
import com.peterlaurence.trekme.core.wmts.domain.model.OsmSourceData
import com.peterlaurence.trekme.core.wmts.domain.model.Outdoors
import com.peterlaurence.trekme.core.wmts.domain.model.SwissTopoData
import com.peterlaurence.trekme.core.wmts.domain.model.UsgsData
import com.peterlaurence.trekme.core.wmts.domain.model.WorldStreetMap
import com.peterlaurence.trekme.core.wmts.domain.model.WorldTopoMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MapDownloadDaoImpl(
    private val settings: Settings
) : MapDownloadDao {
    private val workerCount = 8

    override suspend fun processRequest(
        request: DownloadMapRequest,
        onProgress: (Int) -> Unit
    ): MapDownloadResult = coroutineScope {
        val source = request.source
        val tileSequence = request.mapSpec.tileSequence
        val tileStreamProvider = request.tileStreamProvider

        val threadSafeTileIterator =
            ThreadSafeTileIterator(tileSequence.iterator(), request.numberOfTiles) { p ->
                if (isActive) {
                    onProgress(p.toInt())
                }
            }

        /* Init the progress bar */
        onProgress(0)

        /* Create the destination folder, or else fail-fast */
        val destDir = createDestDir()
            ?: return@coroutineScope MapDownloadResult.Error(MapDownloadStorageError)

        /* A writer which has a folder for each level, and a folder for each row. It does that with
         * using indexes instead of real level, row and col numbers. This greatly simplifies how a
         * tile is later retrieved from a bitmap provider. */
        val tileWriter = TileWriter { tile, bitmap ->
            val tileDir = File(
                destDir,
                tile.indexLevel.toString() + File.separator + tile.indexRow.toString()
            )
            tileDir.mkdirs()
            val tileFile = File(tileDir, tile.indexCol.toString() + ".jpg")
            try {
                val out = FileOutputStream(tileFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                val tag = makeTag(request.source)
                if (tag != null) {
                    out.write(tag)
                }
                out.flush()
                out.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        /* Specific to WorldStreetMap, don't use more than 2 workers */
        val effectiveWorkerCount = if (source == OsmSourceData(WorldStreetMap)) 2 else workerCount
        launchDownloadTask(
            effectiveWorkerCount,
            threadSafeTileIterator,
            tileWriter,
            tileStreamProvider,
            tileSize = request.mapSpec.tileSize
        )
        val map = postProcess(request.mapSpec, source, destDir)

        MapDownloadResult.Success(map)
    }

    private suspend fun postProcess(mapSpec: MapSpec, source: MapSourceData, destDir: File): Map {
        val mapOrigin = when (source) {
            is IgnSourceData -> Ign(licensed = source.layer == IgnClassic)
            IgnSpainData, OrdnanceSurveyData, SwissTopoData, UsgsData -> Wmts(licensed = false)
            is OsmSourceData -> when(source.layer) {
                OpenTopoMap, WorldStreetMap, WorldTopoMap -> Wmts(licensed = false)
                OsmAndHd, Outdoors -> Wmts(licensed = true)
            }
        }

        val map = buildMap(mapSpec, mapOrigin, destDir)

        createNomediaFile(destDir)

        return map
    }

    private suspend fun launchDownloadTask(
        workerCount: Int,
        tileIterator: ThreadSafeTileIterator,
        tileWriter: TileWriter,
        tileStreamProvider: TileStreamProvider,
        tileSize: Int
    ) = coroutineScope {
        for (i in 0 until workerCount) {
            val bitmapProvider = BitmapProvider(tileStreamProvider)
            launchTileDownload(tileIterator, bitmapProvider, tileWriter, tileSize)
        }
    }

    private fun CoroutineScope.launchTileDownload(
        tileIterator: ThreadSafeTileIterator,
        bitmapProvider: BitmapProvider,
        tileWriter: TileWriter,
        tileSize: Int
    ) = launch(Dispatchers.IO) {
        val bitmap: Bitmap = Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.ARGB_8888)
        val options = BitmapFactory.Options()
        options.inBitmap = bitmap
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        bitmapProvider.setBitmapOptions(options)

        while (isActive) {
            val tile = tileIterator.next() ?: break
            bitmapProvider.getBitmap(row = tile.row, col = tile.col, zoomLvl = tile.level).also {
                /* Only write if there was no error */
                if (it != null && isActive) {
                    tileWriter.write(tile, bitmap)
                }
            }
        }
    }

    private suspend fun createDestDir(): File? {
        /* Create a new folder */
        val date = Date()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.ENGLISH)
        val folderName = "map-" + dateFormat.format(date)

        val appDir = settings.getAppDir().firstOrNull() ?: error("App dir should be defined")
        val mapFolder = File(appDir, MAP_FOLDER_NAME)
        val destFolder = File(mapFolder, folderName)

        return if (destFolder.mkdirs()) {
            destFolder
        } else {
            null
        }
    }

    private fun buildMap(
        mapSpec: MapSpec,
        mapOrigin: MapOrigin,
        folder: File,
        imageExtension: String = ".jpg"
    ): Map {

        val levels = (mapSpec.levelMin..mapSpec.levelMax).map {
            Level(it - mapSpec.levelMin, tileSize = Size(mapSpec.tileSize, mapSpec.tileSize))
        }

        val size = Size(mapSpec.mapWidthPx, mapSpec.mapHeightPx)

        val calibration = Calibration(
            MercatorProjection(),
            CalibrationMethod.SIMPLE_2_POINTS,
            mapSpec.calibrationPoints.toList()
        )

        val mapConfig = MapConfig(
            uuid = UUID.randomUUID(),
            name = folder.name, thumbnail = null, thumbnailImage = null,
            levels, mapOrigin, size, imageExtension,
            calibration
        )

        return MapFileBased(mapConfig, folder)
    }
}

private class ThreadSafeTileIterator(
    private val tileIterator: Iterator<Tile>,
    val totalSize: Long,
    val progressListener: (Double) -> Unit
) {
    /* Progress in percent */
    var progress = 0.0
    private var tileIndex: Long = 0

    fun next(): Tile? {
        return synchronized(this) {
            if (tileIterator.hasNext()) {
                updateProgress()
                tileIterator.next()
            } else {
                progress = 100.0
                null
            }
        }
    }

    private fun updateProgress() {
        tileIndex++
        progress = tileIndex * 100.0 / totalSize
        progressListener(progress)
    }
}

private fun interface TileWriter {
    fun write(tile: Tile, bitmap: Bitmap)
}