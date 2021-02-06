package com.peterlaurence.trekme.util.gpx.model

/**
 * The GPX schema, compliant with the Topografix GPX 1.1
 *
 * @author P.Laurence on 30/12/17.
 * @see [Topografix GPX 1.1](http://www.topografix.com/GPX/1/1/)
 */
/* Root tag */
const val TAG_GPX = "gpx"

/* Gpx attributes */
const val ATTR_VERSION = "version"
const val ATTR_CREATOR = "creator"
const val TAG_METADATA = "metadata"
const val TAG_TRACK = "trk"
const val TAG_ROUTE = "rte"
const val TAG_WAYPOINT = "wpt"

/* Common nodes */
const val TAG_NAME = "name"
const val TAG_TIME = "time"

/* Metadata nodes */
const val TAG_BOUNDS = "bounds"
const val TAG_ELE_SOURCE_INFO = "elesrcinfo"
const val ATTR_ELE_SOURCE = "source"
const val ATTR_SAMPLING = "sampling"

/* Track nodes */
const val TAG_SEGMENT = "trkseg"
const val TAG_EXTENSIONS = "extensions"

/* Track segment nodes */
const val TAG_TRK_POINT = "trkpt"

/* Route point, which type is exactly the same as a Track point (see below) */
const val TAG_RTE_POINT = "rtept"

/* Track point, which is a WayPoint type */
const val ATTR_LAT = "lat"
const val ATTR_LON = "lon"
const val TAG_ELEVATION = "ele"

/* Track custom extensions nodes and attributes */
const val TAG_TRACK_STATISTICS = "statistics"
const val ATTR_TRK_STAT_DIST = "distance"
const val ATTR_TRK_STAT_ELE_DIFF_MAX = "eleDiffMax"
const val ATTR_TRK_STAT_ELE_UP_STACK = "eleUpStack"
const val ATTR_TRK_STAT_ELE_DOWN_STACK = "eleDownStack"
const val ATTR_TRK_STAT_DURATION = "duration"
const val ATTR_TRK_STAT_AVG_SPEED = "avgSpeed"
