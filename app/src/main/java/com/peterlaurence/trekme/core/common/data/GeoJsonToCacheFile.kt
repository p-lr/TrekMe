package com.peterlaurence.trekme.core.common.data

import android.net.Uri
import com.peterlaurence.trekme.core.georecord.app.TrekmeFilesProvider
import com.peterlaurence.trekme.core.lib.geojson.GeoJsonWriter
import com.peterlaurence.trekme.core.lib.geojson.model.GeoJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Creates a geojson file from the [geoJson] object, into the [cacheDir].
 * Returns the uri of the file. If the file already exists, it's overwritten.
 */
suspend fun makeGeoJsonUri(
    geoJson: GeoJson,
    cacheDir: File,
    destFileName: String,
    geoJsonWriter: GeoJsonWriter
): Uri? {
    val geoJsonFile = File(cacheDir, destFileName)
    return runCatching {
        withContext(Dispatchers.IO) {
            val created = geoJsonFile.createNewFile()
            if (created || geoJsonFile.exists()) {
                if (geoJsonWriter.writeGeoJson(geoJson, geoJsonFile)) {
                    TrekmeFilesProvider.generateUri(geoJsonFile)
                } else null
            } else null
        }
    }.getOrNull()
}