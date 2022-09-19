package com.peterlaurence.trekme.core.map.data.dao

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.peterlaurence.trekme.core.map.MAP_FILENAME
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.createNomediaFile
import com.peterlaurence.trekme.core.map.domain.dao.MapLoaderDao
import com.peterlaurence.trekme.core.map.domain.dao.MapSaverDao
import com.peterlaurence.trekme.core.map.domain.dao.MapSeekerDao
import com.peterlaurence.trekme.core.map.domain.models.*
import java.io.File
import java.io.FilenameFilter
import java.util.*

/**
 * This [MapSeekerDao] expects a folder structure corresponding to libvips's `dzsave` output.
 */
class MapSeekerDaoImpl(
    private val mapLoaderDao: MapLoaderDao,
    private val mapSaver: MapSaverDao,
    private val fileBasedMapRegistry: FileBasedMapRegistry
) : MapSeekerDao {
    private val thumbnailAcceptSize = 256
    private val IMAGE_EXTENSIONS = arrayOf("jpg", "gif", "png", "bmp", "webp")

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

    private val options = BitmapFactory.Options()
    override var status = MapParseStatus.NO_MAP
        private set

    init {
        options.inJustDecodeBounds = true
    }

    @Throws(FileBasedMapParseException::class, SecurityException::class)
    override suspend fun seek(file: File): Map {
        if (!file.isDirectory) {
            throw FileBasedMapParseException(Issue.NOT_A_DIRECTORY)
        }

        /* Find the first image */
        val imageFile = findFirstImage(file, 0, 5) ?: throw
        FileBasedMapParseException(Issue.NO_IMAGES)

        /* .. use it to deduce the parent folder */
        val parentFolder = findParentFolder(imageFile)
            ?: throw FileBasedMapParseException(Issue.NO_PARENT_FOLDER_FOUND)

        /* Check whether there is already a map.json file or not */
        val existingJsonFile = File(parentFolder, MAP_FILENAME)
        if (existingJsonFile.exists()) {
            val mapList = mapLoaderDao.loadMaps(listOf(parentFolder))
            status = MapParseStatus.EXISTING_MAP
            val map = mapList.firstOrNull()
            if (map != null) {
                /* The nomedia file might already exist, but we do it just in case */
                createNomediaFile(parentFolder)
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
            throw FileBasedMapParseException(Issue.NO_LEVEL_FOUND)
        }

        /* Create provider */
        val mapOrigin = Vips
        val imageExtension = getImageExtension(imageFile) ?: throw FileBasedMapParseException(
            Issue.NO_IMAGES
        )

        /* Map size */
        if (lastLevelTileSize == null) {
            throw FileBasedMapParseException(Issue.NO_LEVEL_FOUND)
        }
        val size = computeMapSize(levelDir, lastLevelTileSize) ?: throw FileBasedMapParseException(
            Issue.MAP_SIZE_INCORRECT
        )

        /* Find a thumbnail */
        val (thumbnailImage, thumbnail) = getThumbnail(parentFolder)

        /* Set the map name to the parent folder name */
        val name = parentFolder.name

        val mapConfig = MapConfig(
            name, thumbnail = thumbnail, levelList, mapOrigin,
            size, imageExtension, calibration = null, sizeInBytes = null
        )

        status = MapParseStatus.NEW_MAP

        val map = Map(mapConfig, thumbnailImage)
        fileBasedMapRegistry.setRootFolder(map.id, parentFolder)
        createNomediaFile(parentFolder)

        mapSaver.save(map)
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

    private fun getThumbnail(mapDir: File): Pair<Bitmap?, String?> {
        for (imageFile in mapDir.listFiles(THUMBNAIL_FILTER) ?: arrayOf()) {
            val bitmap = BitmapFactory.decodeFile(imageFile.path, options)
            if (options.outWidth == thumbnailAcceptSize && options.outHeight == thumbnailAcceptSize) {
                val locale = Locale.getDefault()
                if (!imageFile.name.lowercase(locale)
                        .contains(THUMBNAIL_EXCLUDE_NAME.lowercase(locale))
                ) {
                    return Pair(bitmap, imageFile.name)
                }
            }
        }
        return Pair(null, null)
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

    /**
     * An Exception thrown when an error occurred in a [MapSeekerDao].
     */
    private class FileBasedMapParseException(issue: Issue) :
        MapSeekerDao.MapParseException(issue.name)

    private enum class Issue {
        NOT_A_DIRECTORY,
        NO_PARENT_FOLDER_FOUND,
        NO_LEVEL_FOUND,
        MAP_SIZE_INCORRECT,
        NO_IMAGES
    }

    companion object {
        private const val THUMBNAIL_EXCLUDE_NAME = "blank"
    }
}

private const val TAG = "MapSeekerDaoImpl"