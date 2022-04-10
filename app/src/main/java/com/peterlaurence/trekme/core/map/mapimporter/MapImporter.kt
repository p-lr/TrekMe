package com.peterlaurence.trekme.core.map.mapimporter

import android.graphics.BitmapFactory
import android.util.Log
import com.peterlaurence.trekme.core.map.MAP_FILENAME
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.MapArchive
import com.peterlaurence.trekme.core.map.createNomediaFile
import com.peterlaurence.trekme.core.map.domain.interactors.SaveMapInteractor
import com.peterlaurence.trekme.core.map.domain.interactors.UpdateMapsInteractor
import com.peterlaurence.trekme.core.map.domain.models.Level
import com.peterlaurence.trekme.core.map.domain.models.MapConfig
import com.peterlaurence.trekme.core.map.domain.models.Size
import com.peterlaurence.trekme.core.map.domain.models.Vips
import com.peterlaurence.trekme.core.map.mapimporter.MapImporter.LibvipsMapSeeker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FilenameFilter
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

/**
 * The [MapImporter] exposes a single method : [importFromFile].
 * For instance, [LibvipsMapSeeker] is the only seeker used, because TrekMe only supports one kind
 * of file-based maps.
 * This is typically used after a [MapArchive] has been extracted.
 * TODO: This class should probably be renamed as "MapImportInteractor"
 *
 * @author P.Laurence on 23/06/16 -- Converted to Kotlin on 27/10/19
 */
class MapImporter @Inject constructor(
    private val updateMapsInteractor: UpdateMapsInteractor,
    private val saveMapInteractor: SaveMapInteractor
) {
    companion object {
        private val SEEKER: MapSeeker by lazy { LibvipsMapSeeker() }
        private val THUMBNAIL_ACCEPT_SIZE = 256
        private val IMAGE_EXTENSIONS = arrayOf("jpg", "gif", "png", "bmp", "webp")

        private val TAG = "MapImporter"

        private val THUMBNAIL_FILTER = FilenameFilter { _, filename ->
            IMAGE_EXTENSIONS.any {
                filename.endsWith(".$it")
            }
        }

        private val IMAGE_FILTER = FilenameFilter { dir, filename ->
            /* We only look at files */
            if (File(dir, filename).isDirectory) {
                return@FilenameFilter false
            }

            var accept = true
            for (ext in IMAGE_EXTENSIONS) {
                if (!filename.endsWith(".$ext")) {
                    accept = false
                }
                try {
                    Integer.parseInt(filename.substring(0, filename.lastIndexOf(".")))
                } catch (e: Exception) {
                    accept = false
                }

                if (accept) return@FilenameFilter true
            }
            false
        }

        private val DIR_FILTER = FilenameFilter { dir, filename -> File(dir, filename).isDirectory }
    }

    suspend fun importFromFile(dir: File): MapImportResult {
        return try {
            parseMap(SEEKER, dir)
        } catch (e: MapParseException) {
            Log.e(TAG, "Error parsing $dir (${e.issue})")
            MapImportResult(null, MapParserStatus.NO_MAP)
        }
    }

    private interface MapSeeker {

        val status: MapParserStatus

        /**
         * Produces a [Map] from a given [File].
         */
        @Throws(MapParseException::class)
        suspend fun seek(
            file: File,
            updateMapsInteractor: UpdateMapsInteractor,
            saveMapInteractor: SaveMapInteractor
        ): Map?
    }

    enum class MapParserStatus {
        NO_MAP, // no map could be created
        NEW_MAP, // a new map was successfully created
        EXISTING_MAP, // a map.json file was found
        UNKNOWN_MAP_ORIGIN
    }

    data class MapImportResult(val map: Map?, val status: MapParserStatus)

    /**
     * An Exception thrown when an error occurred in a [MapSeeker].
     */
    class MapParseException internal constructor(internal val issue: Issue) : Exception() {
        internal enum class Issue {
            NOT_A_DIRECTORY,
            NO_PARENT_FOLDER_FOUND,
            NO_LEVEL_FOUND,
            MAP_SIZE_INCORRECT,
            NO_IMAGES
        }
    }

    private suspend fun parseMap(
        mapSeeker: MapSeeker, mDir: File,
    ): MapImportResult = withContext(Dispatchers.IO) {
        val map = mapSeeker.seek(mDir, updateMapsInteractor, saveMapInteractor)
        MapImportResult(map, mapSeeker.status)
    }

    /**
     * This [MapSeeker] expects a folder structure corresponding to libvips's `dzsave` output.
     */
    class LibvipsMapSeeker : MapSeeker {
        private val options = BitmapFactory.Options()
        override var status = MapParserStatus.NO_MAP
            private set

        init {
            options.inJustDecodeBounds = true
        }

        @Throws(MapParseException::class, SecurityException::class)
        override suspend fun seek(
            file: File,
            updateMapsInteractor: UpdateMapsInteractor,
            saveMapInteractor: SaveMapInteractor
        ): Map {
            if (!file.isDirectory) {
                throw MapParseException(MapParseException.Issue.NOT_A_DIRECTORY)
            }

            /* Find the first image */
            val imageFile = findFirstImage(file, 0, 5) ?: throw
            MapParseException(MapParseException.Issue.NO_IMAGES)

            /* .. use it to deduce the parent folder */
            val parentFolder = findParentFolder(imageFile)
                ?: throw MapParseException(MapParseException.Issue.NO_PARENT_FOLDER_FOUND)

            /* Check whether there is already a map.json file or not */
            val existingJsonFile = File(parentFolder, MAP_FILENAME)
            if (existingJsonFile.exists()) {
                val mapList = updateMapsInteractor.updateMaps(listOf(parentFolder))
                status = MapParserStatus.EXISTING_MAP
                val map = mapList.firstOrNull()
                if (map != null) {
                    /* The nomedia file might already exist, but we do it just in case */
                    map.createNomediaFile()
                    return map
                }
            }

            /* Create levels */
            val levelList = ArrayList<Level>()
            val maxLevel = getMaxLevel(parentFolder)
            var levelDir: File? = null
            var lastLevelTileSize: Size? = null  // used later, for the map size
            for (i in 0..maxLevel) {
                levelDir = File(parentFolder, i.toString())
                if (levelDir.exists()) {
                    val tileSize = getTileSize(levelDir) ?: continue
                    lastLevelTileSize = tileSize
                    val level = Level(i, tileSize)
                    levelList.add(level)
                    Log.d(TAG, "creating level $i tileSize $tileSize")
                }
            }

            if (levelDir == null) {
                throw MapParseException(MapParseException.Issue.NO_LEVEL_FOUND)
            }

            /* Create provider */
            val mapOrigin = Vips
            val imageExtension = getImageExtension(imageFile) ?: throw MapParseException(
                MapParseException.Issue.NO_IMAGES
            )

            /* Map size */
            if (lastLevelTileSize == null) {
                throw MapParseException(MapParseException.Issue.NO_LEVEL_FOUND)
            }
            val size = computeMapSize(levelDir, lastLevelTileSize) ?: throw MapParseException(
                MapParseException.Issue.MAP_SIZE_INCORRECT
            )

            /* Find a thumbnail */
            val thumbnailFile = getThumbnail(parentFolder)

            /* Set the map name to the parent folder name */
            val name = parentFolder.name

            val mapConfig = MapConfig(
                name, thumbnail = thumbnailFile?.name, levelList, mapOrigin,
                size, imageExtension, calibration = null, sizeInBytes = null
            )

            /* The json file */
            val jsonFile = File(parentFolder, MAP_FILENAME)

            status = MapParserStatus.NEW_MAP

            val map = Map(mapConfig, jsonFile, thumbnailFile)
            map.createNomediaFile()

            saveMapInteractor.addAndSaveMap(map)
            return map
        }

        /**
         * The map can be contained in a subfolder inside the given directory.
         *
         * @param imageFile an image in the file structure.
         */
        private fun findParentFolder(imageFile: File?): File? {
            if (imageFile != null) {
                try {
                    val parentFolder = imageFile.parentFile?.parentFile?.parentFile
                    if (parentFolder != null && parentFolder.isDirectory) {
                        return parentFolder
                    }
                } catch (e: NullPointerException) {
                    // don't care, will return null
                }
            }
            return null
        }

        /**
         * Find the first image which is named with an integer (so this excludes the thumbnail).
         */
        private fun findFirstImage(dir: File, depth: Int, maxDepth: Int): File? {
            var d = depth
            if (depth > maxDepth) return null

            val listFile = dir.listFiles()
            if (listFile != null) {
                for (aListFile in listFile) {
                    if (aListFile.isDirectory) {
                        val found = findFirstImage(aListFile, d++, maxDepth)
                        if (found != null) {
                            return found
                        }
                    } else {
                        val listImage = dir.listFiles(IMAGE_FILTER)
                        if (!listImage.isNullOrEmpty()) {
                            return listImage[0]
                        }
                    }
                }
            }
            return null
        }

        /* Get the maximum zoom level */
        private fun getMaxLevel(mapDir: File): Int {
            var maxLevel = 0
            var level: Int
            for (f in mapDir.listFiles() ?: arrayOf()) {
                if (f.isDirectory) {
                    try {
                        level = Integer.parseInt(f.name)
                        maxLevel = if (level > maxLevel) level else maxLevel
                    } catch (e: NumberFormatException) {
                        // an unknown folder, ignore it.
                    }

                }
            }
            return maxLevel
        }

        /* We assume that the tile size is constant at a given zoom level */
        private fun getTileSize(levelDir: File): Size? {
            val lineDirList = levelDir.listFiles(DIR_FILTER)
            if (lineDirList.isNullOrEmpty()) {
                return null
            }

            /* take the first line */
            val lineDir = lineDirList[0]
            val imageFiles = lineDir.listFiles(IMAGE_FILTER)
            if (imageFiles != null && imageFiles.isNotEmpty()) {
                val anImage = imageFiles[0]
                BitmapFactory.decodeFile(anImage.path, options)
                return Size(options.outWidth, options.outHeight)
            }

            return null
        }

        /**
         * Get the image extension, width the dot. For example : ".jpg"
         */
        private fun getImageExtension(imageFile: File): String? {
            val imagePath = imageFile.path
            val ext = imagePath.substring(imagePath.lastIndexOf("."))
            return if (ext.isNotEmpty()) {
                ext
            } else null

        }

        private fun getThumbnail(mapDir: File): File? {
            for (imageFile in mapDir.listFiles(THUMBNAIL_FILTER) ?: arrayOf()) {
                BitmapFactory.decodeFile(imageFile.path, options)
                if (options.outWidth == THUMBNAIL_ACCEPT_SIZE && options.outHeight == THUMBNAIL_ACCEPT_SIZE) {
                    val locale = Locale.getDefault()
                    if (!imageFile.name.lowercase(locale)
                            .contains(THUMBNAIL_EXCLUDE_NAME.lowercase(locale))
                    ) {
                        return imageFile
                    }
                }
            }
            return null
        }

        private fun computeMapSize(lastLevel: File, lastLevelTileSize: Size): Size? {
            val lineDirList = lastLevel.listFiles(DIR_FILTER)
            if (lineDirList == null || lineDirList.isEmpty()) {
                return null
            }
            /* Only look into the first line */
            val rowCount = lineDirList.size
            val imageFiles = lineDirList[0].listFiles(IMAGE_FILTER)
            val columnCount = imageFiles?.size ?: return null

            return Size(
                width = columnCount * lastLevelTileSize.width,
                height = rowCount * lastLevelTileSize.height
            )
        }

        companion object {
            private const val THUMBNAIL_EXCLUDE_NAME = "blank"
        }
    }
}
