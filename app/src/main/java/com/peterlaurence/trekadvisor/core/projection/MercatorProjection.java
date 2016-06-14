package com.peterlaurence.trekadvisor.core.projection;

/**
 * Projection named "Popular Visualisation Pseudo-Mercator", used along with EPSG:3857 which is
 * known as "Web Mercator" projected coordinate reference system (CRS).
 * That projected CRS is used for rendering maps in Google Maps, OpenStreetMap, etc.
 *
 * @author peterLaurence
 */
public class MercatorProjection implements Projection {
    public static final transient String NAME = "Pseudo Mercator";
    private transient double mX;
    private transient double mY;
    private transient double mLat;
    private transient double mLng;

    @Override
    public void init() {
        // nothing to do
    }

    /**
     * Conversion from WGS84 coordinates to EPSG:1024 (Popular Visualisation Pseudo-Mercator)
     *
     * @param latitude  latitude in decimal degrees
     * @param longitude longitude in decimal degrees
     */
    @Override
    public void doProjection(double latitude, double longitude) {
        if (Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
            return;
        }
        double num = longitude * 0.017453292519943295; // 2*pi / 360
        double X = 6378137.0 * num;
        double a = latitude * 0.017453292519943295;
        double Y = 3189068.5 * Math.log((1.0 + Math.sin(a)) / (1.0 - Math.sin(a)));
        synchronized (this) {
            mX = X;
            mY = Y;
        }
    }

    /**
     * Conversion from EPSG:1024 coordinates to WGS84 (latitude, longitude).
     *
     * @param mercatorX mercator x in meters
     * @param mercatorY mercator y in meters
     */
    @Override
    public void undoProjection(double mercatorX, double mercatorY) {
        if (Math.abs(mercatorX) < 180 && Math.abs(mercatorY) < 90) {
            //Mercator coordinates not in permissive range (too small)
            return;
        }
        if (Math.abs(mercatorX) > 20037508.3427892 || Math.abs(mercatorY) > 20037508.3427892) {
            //Mercator coordinates not in permissive range (too high)
            return;
        }

        double num3 = mercatorX / 6378137.0;
        double num4 = num3 * 57.295779513082323d;
        double num5 = Math.floor((num4 + 180) / 360.0f);
        synchronized (this) {
            mLng = num4 - (num5 * 360);
            double num6 = 1.5707963267948966d - (2.0 * Math.atan(Math.exp(-mercatorY / 6378137.0)));
            mLat = num6 * 57.295779513082323d;
        }
    }

    /*
     * Ensure we get a coherent couple of values.
     */
    @Override
    public synchronized double[] getProjectedValues() {
        return new double[]{mX, mY};
    }

    @Override
    public double[] getWgs84Coords() {
        return new double[]{mLat, mLng};
    }

    public String getName() {
        return NAME;
    }
}
