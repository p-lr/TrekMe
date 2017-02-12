package com.peterlaurence.trekadvisor.util.gpxparser.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GPX documents contain a metadata header, followed by waypoints, routes, and tracks. <p>
 * Custom elements can be added to the extensions section of the GPX document.
 *
 * @author peterLaurence on 12/02/17.
 */
public class Gpx {
    private final List<Track> mTracks;

    private Gpx(Builder builder) {
        mTracks = Collections.unmodifiableList(new ArrayList<>(builder.mTracks));
    }

    public List<Track> getTracks() {
        return mTracks;
    }

    public static class Builder {
        private List<Track> mTracks;

        public Builder setTracks(List<Track> tracks) {
            mTracks = tracks;
            return this;
        }

        public Gpx build() {
            return new Gpx(this);
        }
    }
}