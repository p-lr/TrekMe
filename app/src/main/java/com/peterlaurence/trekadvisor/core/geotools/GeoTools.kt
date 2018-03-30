package com.peterlaurence.trekadvisor.core.geotools

/**
 * Set of utility functions for geographic computations.
 *
 * @author peterLaurence on 02/07/17.
 */

/**
 * Computes the approximated distance between two points given their latitude and longitude. <br></br>
 * This uses the ‘haversine’ formula to calculate the great-circle distance between the two
 * points – that is, the shortest distance over the earth’s surface, approximated as a sphere –
 * giving an ‘as-the-crow-flies’ distance between the points. <br></br>
 * NB : The average radius of earth is 6371 km.
 *
 * @param lat1 the latitude of the first point
 * @param lon1 the longitude of the first point
 * @param lat2 the latitude of the second point
 * @param lon2 the longitude of the second point
 * @return the distance between the two points, in meters
 */
fun distanceApprox(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val toRad = 0.017453292519943295  // 2*pi/360
    val a = Math.pow(Math.sin((lat2 - lat1) * toRad / 2), 2.0) + Math.cos(lat1 * toRad) * Math.cos(lat2 * toRad) *
            Math.pow(Math.sin((lon2 - lon1) * toRad / 2), 2.0)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return 6371000 * c
}
