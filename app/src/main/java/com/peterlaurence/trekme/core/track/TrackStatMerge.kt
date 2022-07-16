package com.peterlaurence.trekme.core.track

import com.peterlaurence.trekme.core.georecord.domain.model.GeoStatistics
import com.peterlaurence.trekme.core.lib.gpx.model.Bounds

fun List<TrackStatCalculator>.mergeStats(): GeoStatistics {
    return filterNot { it.isEmpty() }.map {
        it.getStatistics()
    }.let {
        GeoStatistics(
            distance = it.sumOf { stat -> stat.distance },
            elevationMax = it.mapNotNull { stat -> stat.elevationMax }.maxOrNull(),
            elevationMin = it.mapNotNull { stat -> stat.elevationMin }.minOrNull(),
            elevationUpStack = it.sumOf { stat -> stat.elevationUpStack },
            elevationDownStack = it.sumOf { stat -> stat.elevationDownStack },
            durationInSecond = it.mapNotNull { stat -> stat.durationInSecond }
                .takeIf { durations -> durations.isNotEmpty() }?.sum(),
            avgSpeed = it.computeAvgSpeed()
        )
    }
}

fun List<TrackStatCalculator>.mergeBounds(): Bounds? {
    return mapNotNull { it.getBounds() }.takeIf { it.isNotEmpty() }?.let {
        Bounds(
            minLat = it.minOf { b -> b.minLat },
            minLon = it.minOf { b -> b.minLon },
            maxLat = it.maxOf { b -> b.maxLat },
            maxLon = it.maxOf { b -> b.maxLon }
        )
    }
}

private fun List<GeoStatistics>.computeAvgSpeed(): Double? {
    var totalDuration = 0L
    val sum = mapNotNull { stat ->
        stat.avgSpeed?.let { avgSpeed ->
            val duration = stat.durationInSecond ?: 0
            totalDuration += duration
            avgSpeed * duration
        }
    }.sum()

    return if (totalDuration > 0L) {
        sum / totalDuration
    } else null
}
