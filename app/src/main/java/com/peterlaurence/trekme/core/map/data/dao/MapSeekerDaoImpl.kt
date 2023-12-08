package com.peterlaurence.trekme.core.map.data.dao

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.peterlaurence.trekme.core.map.data.MAP_FILENAME
import com.peterlaurence.trekme.core.map.data.models.MapFileBased
import com.peterlaurence.trekme.core.map.domain.dao.MapLoaderDao
import com.peterlaurence.trekme.core.map.domain.dao.MapSaverDao
import com.peterlaurence.trekme.core.map.domain.dao.MapSeekerDao
import com.peterlaurence.trekme.core.map.domain.models.Level
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.MapConfig
import com.peterlaurence.trekme.core.map.domain.models.MapParseStatus
import com.peterlaurence.trekme.core.map.domain.models.Size
import com.peterlaurence.trekme.core.map.domain.models.Vips
import com.peterlaurence.trekme.core.map.domain.utils.createNomediaFile
import java.io.File
import java.io.FilenameFilter
import java.util.Locale
import java.util.UUID
import kotlin.math.max

/**
 * This [MapSeekerDao] expects a folder structure corresponding to libvips's `dzsave` output.
 */
class MapSeekerDaoImpl(
    private val mapLoaderDao: MapLoaderDao,
    private val mapSaver: MapSaverDao,
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

    override suspend fun seek(file: File): Result<Map> = runCatching {
        if (!file.isDirectory) {
            throw FileBasedMapParseException(Issue.NOT_A_DIRECTORY)
        }

        /* Find the first image */
        val imageFile =
            findFirstImage(file, 0, 5) ?: throw FileBasedMapParseException(Issue.NO_IMAGES)

        /* .. use it to deduce the parent folder */
        val parentFolder = findParentFolder(imageFile)
            ?: throw FileBasedMapParseException(Issue.NO_PARENT_FOLDER_FOUND)

        /* Check whether there is already a map.json file or not */
        val existingJsonFile = File(parentFolder, MAP_FILENAME)
        if (existingJsonFile.exists()) {
            val mapList = mapLoaderDao.loadMaps(listOf(parentFolder))
            status = MapParseStatus.EXISTING_MAP

            mapList.firstOrNull()?.let {
                /* The nomedia file might already exist, but we do it just in case */
                createNomediaFile(parentFolder)
                return@runCatching it
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

        levelDir ?: throw FileBasedMapParseException(Issue.NO_LEVEL_FOUND)

        /* Create provider */
        val mapOrigin = Vips
        val imageExtension = getImageExtension(imageFile) ?: throw FileBasedMapParseException(
            Issue.NO_IMAGES
        )

        /* Map size */
        lastLevelTileSize ?: throw FileBasedMapParseException(Issue.NO_LEVEL_FOUND)
        val size = computeMapSize(levelDir, lastLevelTileSize) ?: throw FileBasedMapParseException(
            Issue.MAP_SIZE_INCORRECT
        )

        /* Find a thumbnail */
        val (thumbnailImage, thumbnail) = getThumbnail(parentFolder)

        /* Set the map name to the parent folder name */
        val name = parentFolder.name

        val mapConfig = MapConfig(
            uuid = UUID.randomUUID(),
            name, thumbnail = thumbnail, thumbnailImage, levelList, mapOrigin,
            size, imageExtension, calibration = null
        )

        status = MapParseStatus.NEW_MAP

        val map = MapFileBased(mapConfig, parentFolder)
        createNomediaFile(parentFolder)

        mapSaver.save(map)
        map
    }

    /**
     * The map can be contained in a subfolder inside the given directory.
     *
     * @param imageFile an image in the file structure.
     */
    private fun findParentFolder(imageFile: File?): File? {
        return runCatching {
            /* In case of NullPointerException - don't care, will return null */
            val parentFolder = imageFile?.parentFile?.parentFile?.parentFile
            parentFolder?.let {
                if (it.isDirectory) it else null
            }
        }.getOrNull()
    }

    /**
     * Find the first image which is named with an integer (so this excludes the thumbnail).
     */
    private fun findFirstImage(dir: File, depth: Int, maxDepth: Int): File? {
        if (depth > maxDepth) return null
        var currentDepth = depth

        return dir.listFiles()?.let { files ->
            for (file in files) {
                if (file.isDirectory) {
                    findFirstImage(file, currentDepth++, maxDepth)?.also { return@let it }
                } else {
                    dir.listFiles(IMAGE_FILTER)?.elementAtOrNull(0)?.also { return@let it }
                }
            }
            null
        }
    }

    /* Get the maximum zoom level */
    private fun getMaxLevel(mapDir: File): Int {
        var maxLevel = 0
        mapDir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                runCatching {
                    /* In case of NumberFormatException - an unknown folder, ignore it. */
                    maxLevel = max(Integer.parseInt(file.name), maxLevel)
                }
            }
        }
        return maxLevel
    }

    /* We assume that the tile size is constant at a given zoom level */
    private fun getTileSize(levelDir: File): Size? {

        /* take the first line */
        val lineDir = levelDir.listFiles(DIR_FILTER)?.elementAtOrNull(0) ?: return null
        val anImage = lineDir.listFiles(IMAGE_FILTER)?.elementAtOrNull(0) ?: return null

        BitmapFactory.decodeFile(anImage.path, options)
        return Size(options.outWidth, options.outHeight)
    }

    /**
     * Get the image extension, width the dot. For example : ".jpg"
     */
    private fun getImageExtension(imageFile: File): String? {
        val imagePath = imageFile.path
        val ext = imagePath.substring(imagePath.lastIndexOf("."))
        return ext.ifEmpty { null }
    }

    private fun getThumbnail(mapDir: File): Pair<Bitmap?, String?> {

        mapDir.listFiles(THUMBNAIL_FILTER)?.forEach { imageFile ->
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

        val lineDirList = lastLevel.listFiles(DIR_FILTER) ?: return null
        val rowCount = lineDirList.size

        /* Only look into the first line */
        val columnCount =
            lineDirList.elementAtOrNull(0)?.listFiles(IMAGE_FILTER)?.size
                ?: return null

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