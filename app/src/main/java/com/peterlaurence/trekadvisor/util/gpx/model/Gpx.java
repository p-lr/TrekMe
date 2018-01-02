package com.peterlaurence.trekadvisor.util.gpx.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * GPX documents has a version and a creator as attributes, and contains a metadata header,
 * followed by waypoints, routes, and tracks. <p>
 * Custom elements can be added to the extensions section of the GPX document.
 *
 * @author peterLaurence on 12/02/17.
 */
public class Gpx {
    private String mVersion = "1.1";
    private final String mCreator;
    private final List<Track> mTracks;

    private Gpx(Builder builder) {
        mTracks = Collections.unmodifiableList(new ArrayList<>(builder.mTracks));
        mCreator = builder.mCreator;
        mVersion = builder.mVersion;
    }

    public String getVersion() {
        return mVersion;
    }

    public String getCreator() {
        return mCreator;
    }

    public List<Track> getTracks() {
        return mTracks;
    }

    public static class Builder {
        private String mVersion;
        private String mCreator;
        private List<Track> mTracks;

        public Builder setTracks(List<Track> tracks) {
            mTracks = tracks;
            return this;
        }

        public Builder setCreator(String creator) {
            mCreator = creator;
            return this;
        }

        public Builder setVersion(String version) {
            mVersion = version;
            return this;
        }

        public Gpx build() {
            return new Gpx(this);
        }
    }
}