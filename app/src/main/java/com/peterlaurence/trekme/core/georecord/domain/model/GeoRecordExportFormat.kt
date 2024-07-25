package com.peterlaurence.trekme.core.georecord.domain.model

/**
 * Although Gpx is currently the only supported import format, it's possible to specify a different
 * export format.
 */
enum class GeoRecordExportFormat {
    Gpx, GeoJson
}