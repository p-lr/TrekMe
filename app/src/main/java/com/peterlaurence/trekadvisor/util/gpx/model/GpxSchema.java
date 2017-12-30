package com.peterlaurence.trekadvisor.util.gpx.model;

/**
 * The GPX schema, compliant with the Topografix GPX 1.1
 *
 * @author peterLaurence on 30/12/17.
 * @see <a href="http://www.topografix.com/GPX/1/1/">Topografix GPX 1.1</a>
 */
public class GpxSchema {

    /* Root tag */
    public static final String TAG_GPX = "gpx";

    /* Gpx attributes and nodes nodes */
    public static final String ATTR_VERSION = "version";
    public static final String ATTR_CREATOR = "creator";
    public static final String TAG_TRACK = "trk";
    public static final String TAG_ROUTE = "rte";
    public static final String TAG_WAYPOINT = "wpt";

    /* Track nodes */
    public static final String TAG_NAME = "name";
    public static final String TAG_SEGMENT = "trkseg";

    /* Track segment nodes */
    public static final String TAG_POINT = "trkpt";

    /* Track point (but oddly called WayPoint in the GPX1.1 documentation) attributes and nodes */
    public static final String ATTR_LAT = "lat";
    public static final String ATTR_LON = "lon";
    public static final String TAG_ELEVATION = "ele";
    public static final String TAG_TIME = "time";
}
