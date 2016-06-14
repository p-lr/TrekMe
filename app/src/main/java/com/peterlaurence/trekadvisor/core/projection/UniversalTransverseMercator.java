package com.peterlaurence.trekadvisor.core.projection;

import com.google.gson.annotations.SerializedName;

/**
 * The universal transverse Mercator projection is a variant of the Mercator projection.
 * This projection is conformal, so it preserves angles and approximates shape but distorts distance
 * and area.
 * <p>
 * The UTM system divides the Earth between 80°S and 84°N latitude into 60 zones, each 6° of
 * longitude in width. Zone 1 covers longitude 180° to 174° W; zone numbering increases eastward to
 * zone 60, which covers longitude 174° to 180° E.
 * Each of the 60 zones uses a transverse Mercator projection that can map a region of large
 * north-south extent with low distortion.  The point of origin of each UTM zone is the intersection
 * of the equator and the zone's central meridian. To avoid dealing with negative numbers, the
 * central meridian of each zone is defined to coincide with 500000 meters East.
 * <ul>
 * <li>
 * In the northern hemisphere positions are measured northward from zero at the equator.
 * </li>
 * <li>
 * In the southern hemisphere the northing at the equator is set at 10000000 meters so no point has
 * a negative northing value.
 * </li>
 * </ul>
 * </p>
 * Typical manual usage :
 * <pre><code>
 * UniversalTransverseMercator utm = new UniversalTransverseMercator();
 * utm.setZone(31);
 * utm.setHemisphere("N");
 * </code></pre>
 * Note that in TrekAdvisor the zone and hemisphere are set automatically by Gson during
 * deserialization.
 *
 * </pre>
 */
public class UniversalTransverseMercator implements Projection {
    public static final transient String NAME = "Universal Transverse Mercator";

    /* The earth radius in meters */
    private static final transient double a = 6378137;

    /* The earth eccentricity */
    private static final transient double e = 0.081819190842621;

    private static final transient double k0 = 0.9996;

    private static final transient double a1 = 1 - Math.pow(e, 2) / 4 - 3 * Math.pow(e, 4) / 64 - 5 * Math.pow(e, 6) / 256;
    private static final transient double a2 = -3 * Math.pow(e, 2) / 8 - 3 * Math.pow(e, 4) / 32 - 45 * Math.pow(e, 6) / 1024;
    private static final transient double a3 = 15 * Math.pow(e, 4) / 256 + 45 * Math.pow(e, 6) / 1024;
    private static final transient double a4 = -35 * Math.pow(e, 6) / 3072;
    private static final transient double toRad = Math.PI / 180;
    private static final transient double toDecimalDegrees = 180 / Math.PI;

    private transient int FN = 0;
    private transient int FE = 500000; // (meters) by definition in UTM

    /**
     * The zone number is used in {@link #init()} method right after deserialization to determinate
     * the reference meridian.
     */
    @SerializedName("zone")
    private int mZone;

    private transient double mReferenceMeridian;

    /**
     * The hemisphere ("N" or "S") determines the value for the False Northing (FN).
     */
    @SerializedName("hemisphere")
    private String mHemisphere;

    private transient double mEasting;
    private transient double mNorthing;

    private transient double mLatitude;
    private transient double mLongitude;

    @Override
    public void init() {
        setZone(mZone);
        setHemisphere(mHemisphere);
    }

    /**
     * Conversion from WGS84 coordinates to UTM projected values. The computed UTM values are
     * expressed in meters.
     *
     * @param latitude  in decimal degrees
     * @param longitude in decimal degrees
     */
    @Override
    public void doProjection(double latitude, double longitude) {
        /* First, convert values in radians */
        latitude = latitude * toRad;
        longitude = longitude * toRad;

        double nu = 1 / Math.sqrt(1 - Math.pow(e, 2) * Math.pow(Math.sin(latitude), 2));
        double A = (longitude - mReferenceMeridian) * Math.cos(latitude);
        double s = a1 * latitude + a2 * Math.sin(2 * latitude) + a3 * Math.sin(4 * latitude) + a4 * Math.sin(6 * latitude);
        double T = Math.pow(Math.tan(latitude), 2);

        double _e2 = Math.pow(e, 2);
        double C = _e2 / (1 - _e2) * Math.pow(Math.cos(latitude), 2);

        synchronized (this) {
            mEasting = FE + k0 * a * nu * (A + (1 - T + C) * Math.pow(A, 3) / 6 + (5 - 18 * T + Math.pow(T, 2)) *
                    Math.pow(A, 5) / 120);
            mNorthing = FN + k0 * a * (s + nu * Math.tan(latitude) * (Math.pow(A, 2) / 2 + (5 - T + 9 * C + 4 * Math.pow(C, 2)) *
                    Math.pow(A, 4) / 24 + (61 - 58 * T + Math.pow(T, 2)) * Math.pow(A, 6) / 720));
        }
    }

    /**
     * Conversion from UTM to WGS84. The computed latitude and longitude are expressed in decimal
     * degrees.
     *
     * @param E Easting in meters
     * @param N Northing in meters
     */
    @Override
    public void undoProjection(double E, double N) {
        double e1 = (1 - Math.sqrt(1 - e * e)) / (1 + Math.sqrt(1 - e * e));
        double M0 = 0; // UTM
        double M1 = M0 + (N - FN) / k0;
        double µ1 = M1 / (a * (1 - e * e / 4 - 3 * Math.pow(e, 4) / 64 - 5 * Math.pow(e, 6) / 256));
        double phi1 = µ1 + (3 * e1 / 2 - 27 * Math.pow(e1, 3) / 32) * Math.sin(2 * µ1) + (21 * e1 * e1 / 16 -
                55 * Math.pow(e1, 4) / 32) * Math.sin(4 * µ1) + (151 * Math.pow(e1, 3) / 96) * Math.sin(6 * µ1) +
                (1097 * Math.pow(e1, 4) / 512) * Math.sin(8 * µ1);
        double T1 = Math.pow(Math.tan(phi1), 2);
        double ep2 = e * e / (1 - e * e);
        double C1 = ep2 * Math.pow(Math.cos(phi1), 2);
        double nu1 = a / Math.sqrt(1 - e * e * Math.pow(Math.sin(phi1), 2));
        double rho1 = a * (1 - e * e) / Math.pow(1 - e * e * Math.pow(Math.sin(phi1), 2), 1.5);
        double D = (E - FE) / (nu1 * k0);
        synchronized (this) {
            mLatitude = (phi1 - (nu1 * Math.tan(phi1) / rho1) * (D * D / 2 - (5 + 3 * T1 + 10 * C1 -
                    4 * C1 * C1 - 9 * ep2) * Math.pow(D, 4) / 24 + (61 + 90 * T1 + 298 * C1 + 45 * T1 * T1 -
                    252 * ep2 - 3 * C1 * C1) * Math.pow(D, 6) / 720)) * toDecimalDegrees;

            mLongitude = (mReferenceMeridian + (D - (1 + 2 * T1 + C1) * Math.pow(D, 3) / 6 + (5 - 2 * C1 + 28 * T1 - 3 * C1 * C1 +
                    8 * ep2 + 24 * T1 * T1) * Math.pow(D, 5) / 120) / Math.cos(phi1)) * toDecimalDegrees;
        }
    }

    /**
     * Obtain the projected values as an array of two double.
     * The first is the Easting, the second is the Northing.
     * Values are expressed in meters.
     */
    @Override
    public synchronized double[] getProjectedValues() {
        return new double[]{mEasting, mNorthing};
    }

    /**
     * Obtain the WGS84 latitude and longitude as an array of two double.
     * The first is the latitude, the second is the longitude.
     * Values are expressed in radians.
     */
    @Override
    public double[] getWgs84Coords() {
        return new double[]{mLatitude, mLongitude};
    }

    public String getName() {
        return NAME;
    }

    /**
     * Set the UTM zone number 1-60. This is a very important parameter as it determines the
     * reference meridian.
     */
    public void setZone(int zone) {
        int refMeridianInDegrees = -183 + 6 * zone;
        mReferenceMeridian = refMeridianInDegrees * toRad;
    }

    /**
     * Give the information whether to use the North or South version of the projection.
     * <ul>
     * <li>
     * Accepted values for South are "S" and "south" (case insensitive).
     * </li>
     * <li>
     * Accepted values for North are "N" and "north" (case insensitive).
     * </li>
     * </ul>
     */
    public void setHemisphere(String value) {
        if ("S".equalsIgnoreCase(value) || "south".equalsIgnoreCase(value)) {
            FN = 10000000;  // (meters)
        }
        if ("N".equalsIgnoreCase(value) || "north".equalsIgnoreCase(value)) {
            FN = 0;
        }
    }
}