package com.peterlaurence.trekme.core.georecord.data.mapper

import com.peterlaurence.trekme.core.georecord.domain.model.GeoRecord
import com.peterlaurence.trekme.core.lib.geojson.model.Feature
import com.peterlaurence.trekme.core.lib.geojson.model.GeoJson
import com.peterlaurence.trekme.core.lib.geojson.model.Geometry

/**
 * Converting [GeoRecord] to [GeoJson] is a lossy process (we lose timestamps, ids, etc.).
 */
fun GeoRecord.toGeoJson(): GeoJson {
    val markersFeatures = convertMarkers()
    val routesFeatures = convertRoutes()

    return GeoJson(
        features = markersFeatures + routesFeatures
    )
}

private fun GeoRecord.convertMarkers(): List<Feature> {
    return markers.map { m ->
        Feature(
            geometry = Geometry.Point(
                coordinates = listOfNotNull(m.lon, m.lat, m.elevation),
            ),
            properties = mapOf(
                NAME to m.name,
                DESC to m.comment,
            )
        )
    }
}

private fun GeoRecord.convertRoutes(): List<Feature> {
    return routeGroups.map { group ->
        val coordinates = group.routes.map { route ->
            route.routeMarkers.map { m ->
                listOfNotNull(m.lon, m.lat, m.elevation)
            }
        }

        Feature(
            geometry = Geometry.MultiLineString(coordinates),
            properties = mapOf(
                NAME to group.name,
            )
        )
    }
}

private const val NAME = "name"
private const val DESC = "description"