package com.peterlaurence.trekadvisor.core.geotools;

/**
 * Set of utility methods for geographic computations.
 *
 * @author peterLaurence on 02/07/17.
 */
public class GeoTools {
    /**
     * Computes the approximated distance between two points given their latitude and longitude. <br>
     * This uses the ‘haversine’ formula to calculate the great-circle distance between the two
     * points – that is, the shortest distance over the earth’s surface, approximated as a sphere –
     * giving an ‘as-the-crow-flies’ distance between the points. <br>
     * NB : The average radius of earth is 6371 km.
     *
     * @param lat1 the latitude of the first point
     * @param lon1 the longitude of the first point
     * @param lat2 the latitude of the second point
     * @param lon2 the longitude of the second point
     * @return the distance between the two points, in meters
     */
    public static double distanceApprox(double lat1, double lon1, double lat2, double lon2) {
        double to_rad = 0.017453292519943295d;  // 2*pi/360
        double a = Math.pow(Math.sin((lat2 - lat1) * to_rad / 2), 2) +
                Math.cos(lat1 * to_rad) * Math.cos(lat2 * to_rad) *
                        Math.pow(Math.sin((lon2 - lon1) * to_rad / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371000 * c;
    }
}
