package com.peterlaurence.trekadvisor.util.gpx.model

/**
 * The GPX schema, compliant with the Topografix GPX 1.1
 *
 * @author peterLaurence on 30/12/17.
 * @see [Topografix GPX 1.1](http://www.topografix.com/GPX/1/1/)
 */
/* Root tag */
const val TAG_GPX = "gpx"

/* Gpx attributes and nodes nodes */
const val ATTR_VERSION = "version"
const val ATTR_CREATOR = "creator"
const val TAG_TRACK = "trk"
const val TAG_ROUTE = "rte"
const val TAG_WAYPOINT = "wpt"

/* Track nodes */
const val TAG_NAME = "name"
const val TAG_SEGMENT = "trkseg"

/* Track segment nodes */
const val TAG_POINT = "trkpt"

/* Track point (but oddly called WayPoint in the GPX1.1 documentation) attributes and nodes */
const val ATTR_LAT = "lat"
const val ATTR_LON = "lon"
const val TAG_ELEVATION = "ele"
const val TAG_TIME = "time"
