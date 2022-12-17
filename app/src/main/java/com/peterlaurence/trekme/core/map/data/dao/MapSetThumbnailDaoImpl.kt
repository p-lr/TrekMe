package com.peterlaurence.trekme.core.map.data.dao

import android.content.ContentResolver
import android.net.Uri
import com.peterlaurence.trekme.core.map.data.models.MapFileBased
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapSaverDao
import com.peterlaurence.trekme.core.map.domain.dao.MapSetThumbnailDao
import com.peterlaurence.trekme.util.makeThumbnail
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MapSetThumbnailDaoImpl(
    private val defaultDispatcher: CoroutineDispatcher,
    private val mapSaverDao: MapSaverDao,
    private val contentResolver: ContentResolver
) : MapSetThumbnailDao {

    override suspend fun setThumbnail(map: Map, uri: Uri): Result<Map> {
        val directory = (map as? MapFileBased)?.folder ?: return Result.failure(Exception("No map for this id"))
        val targetFile = File(directory, THUMBNAIL_NAME)
        val imageOutputStream: OutputStream = runCatching {
                FileOutputStream(targetFile)
            }.getOrElse {
            return Result.failure(it)
        }

        val thumbnailImage = withContext(defaultDispatcher) {
            makeThumbnail(uri, contentResolver, map.thumbnailSize, imageOutputStream)
        }

        return if (thumbnailImage != null) {
            val newMap = map.copy(
                config = map.configSnapshot.copy(
                    thumbnail = THUMBNAIL_NAME,
                    thumbnailImage = thumbnailImage
                )
            )
            mapSaverDao.save(newMap)
            Result.success(newMap)
        } else {
            Result.failure(Exception("Could not make a thumbnail with $uri"))
        }
    }
}

private const val THUMBNAIL_NAME = "image.jpg"