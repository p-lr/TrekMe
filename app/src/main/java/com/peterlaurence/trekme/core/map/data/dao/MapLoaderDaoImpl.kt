package com.peterlaurence.trekme.core.map.data.dao

import com.google.gson.Gson
import com.peterlaurence.trekme.core.map.MAP_FILENAME
import com.peterlaurence.trekme.core.map.Map
import com.peterlaurence.trekme.core.map.domain.dao.MapLoaderDao
import com.peterlaurence.trekme.core.map.maploader.tasks.mapCreationTask
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class MapLoaderDaoImpl @Inject constructor(
    private val gson: Gson,
    private val defaultDispatcher: CoroutineDispatcher
) : MapLoaderDao {

    /**
     * Parses all [Map]s inside the provided list of directories, then updates the internal list of
     * [Map] : [mapList].
     * It is intended to be the only public method of updating the [Map] list.
     *
     * @param dirs The directories in which to search for maps.
     */
    override suspend fun loadMaps(dirs: List<File>): List<Map> {
        if (dirs.isEmpty()) return emptyList()

        return findMaps(dirs)
    }

    /**
     * Launches the search in background thread.
     *
     * @param dirs The directories in which to search for new maps.
     */
    private suspend fun findMaps(dirs: List<File>) = withContext(defaultDispatcher) {
        mapCreationTask(gson, MAP_FILENAME, *dirs.toTypedArray())
    }
}