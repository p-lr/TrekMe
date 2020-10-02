package com.peterlaurence.trekme.core.geotools

import kotlin.math.*

/**
 * Set of utility functions for geographic computations.
 *
 * @author P.Laurence on 02/07/17.
 */

private const val toRad = 0.017453292519943295 // 2*pi/360

/* WGS 84 ellipsoid parameters */
private const val a: Double = 6_378_137.0       // radius at the equator, in meters
private const val b: Double = 6_356_752.3142    // radius at the poles, in meters
private const val radiusAvg: Double = 6_371_000.0  // radius average

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
    return radiusAvg * c
}

/**
 * Compute the approximated distance between <b>two near points</b>, without taking the elevation
 * into account. The earth is considered as a sphere of 6371 km radius.<br>
 * The precision could be improved with using the radius of the WGS84 ellipsoid at the corresponding
 * latitude, using [earthRadius] function.
 * This formula should not be used for two points separated by a long distance.
 *
 * @param lat1 the latitude of the first point, in decimal degrees
 * @param lon1 the longitude of the first point, in decimal degrees
 * @param lat2 the latitude of the second point, in decimal degrees
 * @param lon2 the longitude of the second point, in decimal degrees
 * @return the distance between the two points, in meters
 */
fun deltaTwoPoints(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val x = radiusAvg * cos(lat1 * toRad) * abs(lon2 - lon1) * toRad
    val y = radiusAvg * abs(lat2 - lat1) * toRad
    return sqrt(x.pow(2) + y.pow(2))
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
    val y = abs(ele2 - ele1)
    return sqrt(x.pow(2) + y.pow(2))
}

/**
 * Given a latitude in decimal degrees, get the radius of the WGS84 ellipsoid.
 * See this [source](https://rechneronline.de/earth-radius/)
 *
 * @param lat the latitude, in decimal degrees
 * @return the radius in meters of the ellipsoid for the given latitude
 */
fun earthRadius(lat: Double): Double {
    val aSq = a.pow(2)
    val bSq = b.pow(2)
    val num = (aSq * cos(lat)).pow(2) + (bSq * sin(lat)).pow(2)
    val den = aSq * cos(lat).pow(2) + bSq * sin(lat).pow(2)
    return sqrt(num / den)
}
