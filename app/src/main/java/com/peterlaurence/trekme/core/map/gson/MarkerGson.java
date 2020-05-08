package com.peterlaurence.trekme.core.map.gson;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author peterLaurence on 30/04/17.
 */
public class MarkerGson {
    public List<Marker> markers;

    public MarkerGson() {
        markers = new ArrayList<>();
    }

    public static class Marker implements Serializable {
        public String name;
        public double lat;
        public double lon;
        public Double elevation;
        public Double proj_x;
        public Double proj_y;
        public String comment;

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Marker)) {
                return false;
            }
            Marker marker = ((Marker) obj);
            return marker.lat == lat && marker.lon == lon;
        }
    }
}
