package com.peterlaurence.trekme.core.map.maploader

import com.peterlaurence.trekme.core.map.*
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.data.*
import com.peterlaurence.trekme.core.projection.MercatorProjection
import com.peterlaurence.trekme.core.projection.Projection
import com.peterlaurence.trekme.core.projection.UniversalTransverseMercator
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class MapLoader(
    private val mainDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
) {
    /**
     * All [Projection]s are registered here.
     */
    private val projectionHashMap = object : HashMap<String, Class<out Projection>>() {
        init {
            put(MercatorProjection.NAME, MercatorProjection::class.java)
            put(UniversalTransverseMercator.NAME, UniversalTransverseMercator::class.java)
        }
    }

    /**
     * Renaming a map involves two steps:
     * 1. Immediately change the name in memory, in the main thread,
     * 2. Rename the directory containing files, using [ioDispatcher],
     * 3. Update the map's directory, if the rename succeeded.
     * After that call, the map.json isn't updated. To update it, invoke [saveMap].
     */
    suspend fun renameMap(map: Map, newName: String) = withContext(mainDispatcher) {
        map.name = newName
        val directory = map.directory ?: return@withContext
        val newDirectory = File(directory.parentFile, newName)
        val renameOk = withContext(ioDispatcher) {
            runCatching {
                directory.renameTo(newDirectory)
            }.getOrNull() ?: false
        }
        if (renameOk) {
            map.directory = newDirectory
        }
    }

    /**
     * Mutate the [Projection] of a given [Map].
     *
     * @return true on success, false if something went wrong.
     */
    fun mutateMapProjection(map: Map, projectionName: String): Boolean {
        val projectionType = projectionHashMap[projectionName] ?: return false
        try {
            val projection = projectionType.newInstance()
            map.projection = projection
        } catch (e: InstantiationException) {
            // wrong projection name
            return false
        } catch (e: IllegalAccessException) {
            return false
        } catch (e: ExceptionInInitializerError) {
            return false
        }

        return true
    }

    interface MapArchiveListUpdateListener {
        fun onMapArchiveListUpdate(mapArchiveList: List<MapArchive>)
    }
}
