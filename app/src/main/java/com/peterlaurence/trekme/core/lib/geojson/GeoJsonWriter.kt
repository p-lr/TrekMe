package com.peterlaurence.trekme.core.lib.geojson

import com.peterlaurence.trekme.core.lib.geojson.model.GeoJson
import com.peterlaurence.trekme.util.FileUtils
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class GeoJsonWriter {
    private val json = Json { isLenient = true; ignoreUnknownKeys = true }

    /**
     * Shall be called off ui thread.
     */
    fun writeGeoJson(geoJson: GeoJson, outputFile: File): Boolean {
        val content = json.encodeToString<GeoJson>(geoJson)
        return FileUtils.writeToFile(content, outputFile)
    }
}

const val GEOJSON_FILE_EXT = "geojson"