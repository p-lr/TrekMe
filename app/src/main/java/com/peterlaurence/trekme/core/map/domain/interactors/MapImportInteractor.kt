package com.peterlaurence.trekme.core.map.domain.interactors

import android.util.Log
import com.peterlaurence.trekme.core.map.domain.dao.MapSeekerDao
import com.peterlaurence.trekme.core.map.domain.models.MapImportResult
import com.peterlaurence.trekme.core.map.domain.models.MapParseStatus
import com.peterlaurence.trekme.core.map.domain.repository.MapRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * Import a map or a list of map from a folder. Depending on the implementation of the [MapSeekerDao],
 * we look for a particular file structure or other ways to save maps.
 *
 * @since 23/06/16 -- Converted to Kotlin on 27/10/19
 */
class MapImportInteractor @Inject constructor(
    private val mapRepository: MapRepository,
    private val mapSeekerDao: MapSeekerDao
) {
    suspend fun importFromFile(dir: File): MapImportResult {
        return try {
            parseMap(mapSeekerDao, dir)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing $dir (${e.message})")
            MapImportResult(null, MapParseStatus.NO_MAP)
        }
    }


    private suspend fun parseMap(
        mapSeekerDao: MapSeekerDao, mDir: File,
    ): MapImportResult = withContext(Dispatchers.IO) {
        val map = mapSeekerDao.seek(mDir)
        if (map != null) {
            mapRepository.addMaps(listOf(map))
        }

        MapImportResult(map, mapSeekerDao.status)
    }
}

private const val TAG = "MapImporter"
