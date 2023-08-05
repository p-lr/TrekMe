package com.peterlaurence.trekme.core.map.data.dao

import com.peterlaurence.trekme.core.map.data.models.MapFileBased
import com.peterlaurence.trekme.core.map.data.models.ignTag
import com.peterlaurence.trekme.core.map.data.models.osmHdStandardTag
import com.peterlaurence.trekme.core.map.domain.dao.MapTagDao
import com.peterlaurence.trekme.core.map.domain.models.Map
import com.peterlaurence.trekme.core.map.domain.models.TileTag
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile

class MapTagDaoImpl(private val ioDispatcher: CoroutineDispatcher) : MapTagDao {

    override suspend fun getTag(map: Map): TileTag? = withContext(ioDispatcher){
        val directory = (map as? MapFileBased)?.folder ?: return@withContext null

        runCatching {
            val tileDirs = directory.listFiles { path ->
                path.isDirectory && path.name.toIntOrNull() != null
            }

            val lastFolder = tileDirs?.maxBy { it.name } ?: return@withContext null

            val lines = lastFolder.listFiles { path ->
                path.isDirectory && path.name.toIntOrNull() != null
            }

            val lastLine = lines?.maxBy { it.name } ?: return@withContext null
            val lastTile = lastLine.listFiles()?.maxBy { it.name } ?: return@withContext null

            val raf = RandomAccessFile(lastTile, "r")
            raf.seek(lastTile.length() - 2)
            val tag = ByteArray(2)
            raf.readFully(tag)

            when {
                tag.contentEquals(ignTag) -> TileTag.IGN
                tag.contentEquals(osmHdStandardTag) -> TileTag.OsmHdStandard
                else -> null
            }
        }.getOrNull()
    }
}