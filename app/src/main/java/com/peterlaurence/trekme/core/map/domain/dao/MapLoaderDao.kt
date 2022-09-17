package com.peterlaurence.trekme.core.map.domain.dao

import com.peterlaurence.trekme.core.map.Map
import java.io.File

/**
 * In TrekMe current design, maps are retrieved locally.
 * While the current map persistence format is file based, future format might eventually use
 * databases. However, even databases will be stored locally and will be lookout as [File]s.
 * So the [File] in this interface refers to where the map source (a database or a file structure)
 * is located.
 */
interface MapLoaderDao {
    suspend fun loadMaps(dirs: List<File>): List<Map>
}