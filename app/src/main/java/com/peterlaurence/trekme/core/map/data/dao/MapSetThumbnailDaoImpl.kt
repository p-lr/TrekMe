package com.peterlaurence.trekme.core.map.data.dao

import android.content.ContentResolver
import android.net.Uri
import com.peterlaurence.trekme.core.map.data.THUMBNAIL_NAME
import com.peterlaurence.trekme.core.map.data.THUMBNAIL_SIZE
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

    override suspend fun setThumbnail(map: Map, uri: Uri): Boolean {
        val directory = (map as? MapFileBased)?.folder ?: return false
        val targetFile = File(directory, THUMBNAIL_NAME)
        val imageOutputStream: OutputStream = runCatching {
                FileOutputStream(targetFile)
            }.getOrElse {
            return false
        }

        val thumbnailImage = withContext(defaultDispatcher) {
            makeThumbnail(uri, contentResolver, THUMBNAIL_SIZE, imageOutputStream)
        }

        return if (thumbnailImage != null) {
            map.thumbnail.value = thumbnailImage
            mapSaverDao.save(map)
            true
        } else false
    }
}
