package com.peterlaurence.trekadvisor.core.geotools

import kotlin.math.pow

/**
 * Set of utility functions for geographic computations.
 *
 * @author peterLaurence on 02/07/17.
 */

private const val toRad = 0.017453292519943295 // 2*pi/360

/**
 * Computes the approximated distance between two points given their latitude and longitude. <br></br>
 * This uses the ‘haversine’ formula to calculate the great-circle distance between the two
 * points – that is, the shortest distance over the earth’s surface, approximated as a sphere –
 * giving an ‘as-the-crow-flies’ distance between the points. <br></br>
 * This formula works even if the two points are separated by a long distance.<br>
 * NB : The average radius of earth is 6371 km.
 *
 * @param lat1 the latitude of the first point, in decimal degrees
 * @param lon1 the longitude of the first point, in decimal degrees
 * @param lat2 the latitude of the second point, in decimal degrees
 * @param lon2 the longitude of the second point, in decimal degrees
 * @return the distance between the two points, in meters
 */
fun distanceApprox(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val a = Math.pow(Math.sin((lat2 - lat1) * toRad / 2), 2.0) + Math.cos(lat1 * toRad) * Math.cos(lat2 * toRad) *
            Math.pow(Math.sin((lon2 - lon1) * toRad / 2), 2.0)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return 6371000 * c
}

/**
 * Compute the approximated distance between <b>two near points</b>, without taking the elevation
 * into account. The earth is considered as a sphere of 6371 km radius.<br>
 * This formula should not be used for two points separated by a long distance.
 *
 * @param lat1 the latitude of the first point, in decimal degrees
 * @param lon1 the longitude of the first point, in decimal degrees
 * @param lat2 the latitude of the second point, in decimal degrees
 * @param lon2 the longitude of the second point, in decimal degrees
 * @return the distance between the two points, in meters
 */
fun deltaTwoPoints(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371000
    val x = r * Math.cos(lat1 * toRad) * Math.abs(lon2 - lon1) * toRad
    val y = r * Math.abs(lat2 - lat1) * toRad
    return Math.sqrt(x.pow(2) + y.pow(2))
}

/**
 * Compute the approximated distance between <b>two near points</b>, taking the elevation into account.
 * <br> This formula should not be used for two points separated by a long distance.
 *
 * @param lat1 the latitude of the first point, in decimal degrees
 * @param lon1 the longitude of the first point, in decimal degrees
 * @param ele1 the elevation of the first point, in meters
 * @param lat2 the latitude of the second point, in decimal degrees
 * @param lon2 the longitude of the second point, in decimal degrees
 * @param ele2 the elevation of the second point, in meters
 * @return the distance between the two points, in meters
 */
fun deltaTwoPoints(lat1: Double, lon1: Double, ele1: Double, lat2: Double, lon2: Double, ele2: Double): Double {
    val x = deltaTwoPoints(lat1, lon1, lat2, lon2)
    val y = Math.abs(ele2 - ele1)
    return Math.sqrt(x.pow(2) + y.pow(2))
}
