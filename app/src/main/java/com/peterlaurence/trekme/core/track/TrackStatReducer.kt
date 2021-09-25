package com.peterlaurence.trekme.core.track

fun List<TrackStatCalculator>.reduce(): TrackStatistics {
    filterNot { it.isEmpty() }.map {
        it.getStatistics()
    }
    TODO("stats should contains min and max elevations, not elevation difference")
}